package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.Connection;
import es.upm.miw.oneleft.social.domain.model.Follow;
import es.upm.miw.oneleft.social.domain.model.FollowCounts;
import es.upm.miw.oneleft.social.domain.model.FollowStatus;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.model.Person;
import es.upm.miw.oneleft.social.domain.port.out.Follows;
import es.upm.miw.oneleft.social.domain.port.out.People;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Output adapter: people and follows in PostgreSQL. */
@Repository
public class JpaSocialRepository implements People, Follows {

    private final SpringDataPersonRepository people;
    private final SpringDataFollowRepository follows;

    public JpaSocialRepository(SpringDataPersonRepository people, SpringDataFollowRepository follows) {
        this.people = people;
        this.follows = follows;
    }

    private static PageResult<Connection> page(Page<ConnectionRow> rows, PageQuery page) {
        return new PageResult<>(rows.map(ConnectionRow::toDomain).getContent(), page.page(), page.size(),
                rows.getTotalElements());
    }

    @Override
    public Optional<Person> find(UUID userId) {
        return people.findById(userId).map(PersonEntity::toDomain);
    }

    @Override
    public Person save(Person person) {
        return people.save(PersonEntity.of(person)).toDomain();
    }

    @Override
    public Optional<Follow> find(UUID followerId, UUID followeeId) {
        return follows.findById(new FollowEntity.Key(followerId, followeeId)).map(FollowEntity::toDomain);
    }

    @Override
    public Follow save(Follow follow) {
        return follows.save(FollowEntity.of(follow)).toDomain();
    }

    @Override
    public void delete(UUID followerId, UUID followeeId) {
        follows.deleteById(new FollowEntity.Key(followerId, followeeId));
    }

    @Override
    public void acceptAllRequests(UUID followeeId, Instant at) {
        follows.acceptAll(followeeId, at);
    }

    @Override
    public PageResult<Connection> followersOf(UUID userId, FollowStatus status, PageQuery page) {
        return page(follows.followersOf(userId, status, PageRequest.of(page.page(), page.size())), page);
    }

    @Override
    public PageResult<Connection> followedBy(UUID userId, FollowStatus status, PageQuery page) {
        return page(follows.followedBy(userId, status, PageRequest.of(page.page(), page.size())), page);
    }

    @Override
    public FollowCounts counts(UUID userId) {
        var counts = follows.counts(userId);
        return new FollowCounts(counts.getFollowers(), counts.getFollowing(), counts.getRequests(),
                counts.getSent());
    }
}
