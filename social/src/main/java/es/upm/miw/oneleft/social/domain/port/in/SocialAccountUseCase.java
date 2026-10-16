package es.upm.miw.oneleft.social.domain.port.in;

import es.upm.miw.oneleft.social.domain.model.Caller;
import es.upm.miw.oneleft.social.domain.model.FeedAudience;
import es.upm.miw.oneleft.social.domain.model.PersonView;
import es.upm.miw.oneleft.social.domain.model.SocialAccount;
import es.upm.miw.oneleft.social.domain.model.Visibility;

import java.util.UUID;

/** My profile in the social service and how I see someone else (HU-047). */
public interface SocialAccountUseCase {

    /** My account, created private with the name of my account the first time. */
    SocialAccount myAccount(Caller me);

    /** Public or private. Opening a private profile accepts the requests that were waiting. */
    SocialAccount changeVisibility(Caller me, Visibility visibility);

    /** Who sees my activity in their feeds (HU-059): my followers or nobody. */
    SocialAccount changeFeedAudience(Caller me, FeedAudience audience);

    PersonView person(Caller me, UUID userId);
}
