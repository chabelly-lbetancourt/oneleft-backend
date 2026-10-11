package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Place;
import es.upm.miw.oneleft.plans.domain.model.PlaceQuery;
import es.upm.miw.oneleft.plans.domain.model.PlaceSuggestion;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/** HU-071: suggestions for the meeting point, closest first and only around. */
class PlaceSuggestionServiceTest {

    private static final PlaceQuery QUERY = new PlaceQuery("polideportivo", 40.39, -3.63);

    private final List<PlaceQuery> asked = new ArrayList<>();
    private List<Place> answer = List.of();
    private final PlaceSuggestionService service = new PlaceSuggestionService(query -> {
        asked.add(query);
        return answer;
    });

    /** A place this many thousandths of a degree north of the device: about 111 m each. */
    private static Place north(String name, int thousandths) {
        return new Place(name, "Madrid", 40.39 + thousandths / 1000.0, -3.63);
    }

    @Test
    void theClosestPlacesComeFirstWithTheirDistance() {
        answer = List.of(north("Polideportivo Palomeras", 20), north("Polideportivo Campus Sur", 3));

        assertThat(service.suggest(QUERY))
                .extracting(suggestion -> suggestion.place().name(), PlaceSuggestion::distanceMeters)
                .containsExactly(tuple("Polideportivo Campus Sur", 334),
                        tuple("Polideportivo Palomeras", 2224));
        assertThat(asked).containsExactly(QUERY);
    }

    @Test
    void farAwayPlacesWithTheSameNameAreLeftOut() {
        var valencia = new Place("Polideportivo Nazaret", "Valencia", 39.45, -0.33);
        answer = List.of(valencia, north("Polideportivo Vallecas", 400));

        assertThat(service.suggest(QUERY)).extracting(suggestion -> suggestion.place().name())
                .containsExactly("Polideportivo Vallecas");
    }

    @Test
    void theSamePlaceTwiceIsSuggestedOnceAtItsClosestPoint() {
        answer = List.of(north("Polideportivo Campus Sur", 5), north("Polideportivo Campus Sur", 3));

        assertThat(service.suggest(QUERY)).singleElement()
                .satisfies(suggestion -> assertThat(suggestion.distanceMeters()).isEqualTo(334));
    }

    @Test
    void atMostFivePlacesFitUnderTheField() {
        answer = IntStream.rangeClosed(1, 8).mapToObj(i -> north("Place " + i, i)).toList();

        assertThat(service.suggest(QUERY)).extracting(suggestion -> suggestion.place().name())
                .containsExactly("Place 1", "Place 2", "Place 3", "Place 4", "Place 5");
    }

    @Test
    void withoutAnswerThereAreNoSuggestions() {
        assertThat(service.suggest(QUERY)).isEmpty();
    }
}
