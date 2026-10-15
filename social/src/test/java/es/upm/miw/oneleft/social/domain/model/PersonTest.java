package es.upm.miw.oneleft.social.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonTest {

    private static final UUID LUCIA = UUID.randomUUID();

    @Test
    void theNameIsTrimmedAndCutToFiftyCharacters() {
        assertThat(new Person(LUCIA, "  Lucía  ", Visibility.PRIVATE).name()).isEqualTo("Lucía");
        assertThat(new Person(LUCIA, "x".repeat(60), Visibility.PRIVATE).name()).hasSize(Person.MAX_NAME_LENGTH);
    }

    @Test
    void aPersonNeedsAnIdANameAndAVisibility() {
        assertThatThrownBy(() -> new Person(LUCIA, " ", Visibility.PUBLIC))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("person.name"));
        assertThatThrownBy(() -> new Person(null, "Lucía", Visibility.PUBLIC))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Person(LUCIA, "Lucía", null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void renamingToTheSameNameKeepsTheSamePerson() {
        var lucia = new Person(LUCIA, "Lucía", Visibility.PRIVATE);

        assertThat(lucia.renamed(" Lucía ")).isSameAs(lucia);
        assertThat(lucia.renamed("Lucía M.").name()).isEqualTo("Lucía M.");
        assertThat(lucia.withVisibility(Visibility.PUBLIC).visibility()).isEqualTo(Visibility.PUBLIC);
    }

    @Test
    void thePlansAndTheNetworkOfSomeoneAreSeenByThemTheirFollowersOrEveryoneIfTheirProfileIsPublic() {
        var closed = new Person(UUID.randomUUID(), "Ana", Visibility.PRIVATE);
        var open = closed.withVisibility(Visibility.PUBLIC);

        assertThat(new PersonView(closed, 0, 0, Relation.NONE, false).activityVisible()).isFalse();
        assertThat(new PersonView(closed, 0, 0, Relation.REQUESTED, false).activityVisible()).isFalse();
        assertThat(new PersonView(closed, 0, 0, Relation.FOLLOWING, false).activityVisible()).isTrue();
        assertThat(new PersonView(closed, 0, 0, Relation.SELF, false).activityVisible()).isTrue();
        assertThat(new PersonView(open, 0, 0, Relation.NONE, false).activityVisible()).isTrue();
    }
}
