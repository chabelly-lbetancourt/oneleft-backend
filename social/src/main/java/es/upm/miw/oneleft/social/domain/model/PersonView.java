package es.upm.miw.oneleft.social.domain.model;

/**
 * Someone as I see them: their name, who may follow them, how many follow them and they follow, whether I follow them
 * and whether they follow me.
 */
public record PersonView(Person person, long followers, long following, Relation relation, boolean followsMe) {
}
