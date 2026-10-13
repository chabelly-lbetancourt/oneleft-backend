package es.upm.miw.oneleft.social.domain.model;

/** My place in the social service: my name, who may follow me and my counts. */
public record SocialAccount(Person person, FollowCounts counts) {
}
