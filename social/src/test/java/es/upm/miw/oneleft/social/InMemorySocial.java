package es.upm.miw.oneleft.social;

import es.upm.miw.oneleft.social.domain.model.Connection;
import es.upm.miw.oneleft.social.domain.model.Follow;
import es.upm.miw.oneleft.social.domain.model.FollowCounts;
import es.upm.miw.oneleft.social.domain.model.FollowStatus;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.model.Person;
import es.upm.miw.oneleft.social.domain.port.out.Follows;
import es.upm.miw.oneleft.social.domain.port.out.People;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/** People and follows in memory, with the same rules as the SQL queries (tested in SocialIntegrationTest). */
public class InMemorySocial implements People, Follows {

    public final Map<UUID, Person> people = new HashMap<>();
    public final Map<String, Follow> follows = new HashMap<>();
    public int saves;

    private static String key(UUID followerId, UUID followeeId) {
        return followerId + ">" + followeeId;
    }

    @Override
    public Optional<Person> find(UUID userId) {
        return Optional.ofNullable(people.get(userId));
    }

    @Override
    public Person save(Person person) {
        saves++;
        people.put(person.userId(), person);
        return person;
    }

    @Override
    public Optional<Follow> find(UUID followerId, UUID followeeId) {
        return Optional.ofNullable(follows.get(key(followerId, followeeId)));
    }

    @Override
    public Follow save(Follow follow) {
        follows.put(key(follow.followerId(), follow.followeeId()), follow);
        return follow;
    }

    @Override
    public void delete(UUID followerId, UUID followeeId) {
        follows.remove(key(followerId, followeeId));
    }

    @Override
    public void acceptAllRequests(UUID followeeId, Instant at) {
        new ArrayList<>(follows.values()).stream()
                .filter(follow -> follow.followeeId().equals(followeeId) && follow.isPending())
                .forEach(follow -> save(follow.accept(Clock.fixed(at, ZoneOffset.UTC))));
    }

    @Override
    public PageResult<Connection> followersOf(UUID userId, FollowStatus status, PageQuery page) {
        return list(follow -> follow.followeeId().equals(userId) && follow.status() == status, Follow::followerId,
                page);
    }

    @Override
    public PageResult<Connection> followedBy(UUID userId, FollowStatus status, PageQuery page) {
        return list(follow -> follow.followerId().equals(userId) && follow.status() == status, Follow::followeeId,
                page);
    }

    @Override
    public FollowCounts counts(UUID userId) {
        var all = follows.values();
        return new FollowCounts(
                all.stream().filter(f -> f.followeeId().equals(userId) && !f.isPending()).count(),
                all.stream().filter(f -> f.followerId().equals(userId) && !f.isPending()).count(),
                all.stream().filter(f -> f.followeeId().equals(userId) && f.isPending()).count(),
                all.stream().filter(f -> f.followerId().equals(userId) && f.isPending()).count());
    }

    private PageResult<Connection> list(Predicate<Follow> which, Function<Follow, UUID> other, PageQuery page) {
        List<Connection> all = follows.values().stream().filter(which)
                .map(follow -> Connection.of(people.get(other.apply(follow)), follow))
                .sorted(Comparator.comparing(Connection::since).reversed().thenComparing(Connection::name))
                .toList();
        var from = Math.min(all.size(), page.page() * page.size());
        return new PageResult<>(all.subList(from, Math.min(all.size(), from + page.size())), page.page(), page.size(),
                all.size());
    }
}
