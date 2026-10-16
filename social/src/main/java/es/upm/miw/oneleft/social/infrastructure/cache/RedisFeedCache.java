package es.upm.miw.oneleft.social.infrastructure.cache;

import es.upm.miw.oneleft.social.domain.model.FeedItem;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.port.out.FeedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output adapter: the first page of each feed in Redis, as JSON, for {@link #TTL} at most (HU-051). It is thrown away
 * once a change is saved: thrown away before, a feed read in between could keep the old page. Redis is a shortcut,
 * never a requirement: if it does not answer, the feed is read from the database.
 */
@Component
public class RedisFeedCache implements FeedCache {

    static final String PREFIX = "oneleft:social:feed:";
    static final Duration TTL = Duration.ofMinutes(10);
    private static final Logger log = LoggerFactory.getLogger(RedisFeedCache.class);

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    public RedisFeedCache(StringRedisTemplate redis, JsonMapper json) {
        this.redis = redis;
        this.json = json;
    }

    private static String key(UUID viewer) {
        return PREFIX + viewer;
    }

    @Override
    public Optional<PageResult<FeedItem>> firstPage(UUID viewer) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(key(viewer)))
                    .map(cached -> json.readValue(cached, CachedPage.class).toDomain());
        } catch (RuntimeException e) {
            log.warn("Feed cache unavailable, reading the database: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void keepFirstPage(UUID viewer, PageResult<FeedItem> page) {
        try {
            redis.opsForValue().set(key(viewer), json.writeValueAsString(CachedPage.of(page)), TTL);
        } catch (RuntimeException e) {
            log.warn("Feed cache unavailable, the page is not kept: {}", e.getMessage());
        }
    }

    @Override
    public void forget(Collection<UUID> viewers) {
        if (viewers.isEmpty()) {
            return;
        }
        var keys = viewers.stream().map(RedisFeedCache::key).toList();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delete(keys);
                }
            });
        } else {
            delete(keys);
        }
    }

    private void delete(List<String> keys) {
        try {
            redis.delete(keys);
        } catch (RuntimeException e) {
            // The page expires anyway: at most TTL late
            log.warn("Feed cache unavailable, {} pages expire on their own: {}", keys.size(), e.getMessage());
        }
    }

    /** What is kept: always the first page, of the default size. */
    record CachedPage(List<FeedItem> items, long total) {

        static CachedPage of(PageResult<FeedItem> page) {
            return new CachedPage(page.items(), page.total());
        }

        PageResult<FeedItem> toDomain() {
            return new PageResult<>(items, 0, PageQuery.DEFAULT_SIZE, total);
        }
    }
}
