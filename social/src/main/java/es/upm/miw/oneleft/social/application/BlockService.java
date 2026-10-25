package es.upm.miw.oneleft.social.application;

import es.upm.miw.oneleft.social.domain.model.BlockedPerson;
import es.upm.miw.oneleft.social.domain.model.Caller;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.model.ValidationException;
import es.upm.miw.oneleft.social.domain.port.in.BlockUseCase;
import es.upm.miw.oneleft.social.domain.port.out.Blocks;
import es.upm.miw.oneleft.social.domain.port.out.Directory;
import es.upm.miw.oneleft.social.domain.port.out.FeedCache;
import es.upm.miw.oneleft.social.domain.port.out.Follows;
import es.upm.miw.oneleft.social.domain.port.out.People;
import es.upm.miw.oneleft.social.domain.port.out.SafetyEvents;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Blocking people (HU-033). A block ends the follows and the requests between both, in both directions, so neither
 * feed shows the other one; the plans and notifications services learn it from the event. Nothing tells the blocked
 * person: blocking or unblocking again changes nothing.
 */
@Service
@Transactional
public class BlockService implements BlockUseCase {

    private final Registration registration;
    private final Follows follows;
    private final Blocks blocks;
    private final FeedCache feeds;
    private final SafetyEvents events;
    private final Clock clock;

    public BlockService(People people, Directory directory, Follows follows, Blocks blocks, FeedCache feeds,
                        SafetyEvents events, Clock clock) {
        this.registration = new Registration(people, directory);
        this.follows = follows;
        this.blocks = blocks;
        this.feeds = feeds;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void block(Caller me, UUID userId) {
        if (me.userId().equals(userId)) {
            throw new ValidationException("block.self", "Nobody blocks themselves");
        }
        registration.register(me);
        // Kept with their name for my list of blocked people (and unknown accounts are a 404)
        registration.known(userId);
        follows.delete(me.userId(), userId);
        follows.delete(userId, me.userId());
        feeds.forget(List.of(me.userId(), userId));
        var at = clock.instant();
        if (blocks.block(me.userId(), userId, at)) {
            events.blocked(me.userId(), userId, at);
        }
    }

    @Override
    public void unblock(UUID me, UUID userId) {
        if (blocks.unblock(me, userId)) {
            events.unblocked(me, userId, clock.instant());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BlockedPerson> blocked(UUID me, PageQuery page) {
        return blocks.blockedBy(me, page);
    }
}
