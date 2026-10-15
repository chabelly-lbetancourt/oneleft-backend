package es.upm.miw.oneleft.social.domain.model;

/**
 * Someone as I see them: their name, who may follow them, how many follow them and they follow, whether I follow them
 * and whether they follow me.
 */
public record PersonView(Person person, long followers, long following, Relation relation, boolean followsMe) {

    /**
     * Whether I may see their recent plans, their followers and who they follow (HU-048): it is me, their profile is
     * public, or I follow them. Name, photo, bio, hobbies, statistics and reputation are seen by everyone.
     */
    public boolean activityVisible() {
        return relation == Relation.SELF || relation == Relation.FOLLOWING || person.visibility() == Visibility.PUBLIC;
    }
}
