package es.upm.miw.oneleft.social.application;

import es.upm.miw.oneleft.social.domain.port.in.PersonalDataUseCase;
import es.upm.miw.oneleft.social.domain.port.out.FeedCache;
import es.upm.miw.oneleft.social.domain.port.out.Follows;
import es.upm.miw.oneleft.social.domain.port.out.PersonalData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The personal data of the social service (HU-045). Erasing someone also clears the cached feeds that showed what
 * they did: their followers' first page, and their own.
 */
@Service
@Transactional
public class PersonalDataService implements PersonalDataUseCase {

    private final PersonalData data;
    private final Follows follows;
    private final FeedCache feeds;

    public PersonalDataService(PersonalData data, Follows follows, FeedCache feeds) {
        this.data = data;
        this.follows = follows;
        this.feeds = feeds;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<Map<String, Object>>> export(UUID userId) {
        return data.of(userId);
    }

    @Override
    public void erase(UUID userId) {
        var viewers = new ArrayList<>(follows.acceptedFollowerIds(userId));
        viewers.add(userId);
        data.erase(userId);
        feeds.forget(viewers);
    }
}
