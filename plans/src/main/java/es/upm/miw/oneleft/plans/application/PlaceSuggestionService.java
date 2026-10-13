package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.PlaceQuery;
import es.upm.miw.oneleft.plans.domain.model.PlaceSuggestion;
import es.upm.miw.oneleft.plans.domain.port.in.SuggestPlacesUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.PlaceSearch;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Place suggestions for the meeting point (HU-071). The geocoder weighs the position but can still return far away
 * places with the same name: here they are measured, sorted by distance and left out beyond the area of the plans.
 */
@Service
public class PlaceSuggestionService implements SuggestPlacesUseCase {

    /** Enough for a phone screen under the field. */
    public static final int MAX_SUGGESTIONS = 5;
    /** Plans are for people nearby: a place farther away is not a meeting point for them. */
    public static final int MAX_DISTANCE_METERS = 50_000;

    private final PlaceSearch places;

    public PlaceSuggestionService(PlaceSearch places) {
        this.places = places;
    }

    @Override
    public List<PlaceSuggestion> suggest(PlaceQuery query) {
        var closestFirst = places.search(query).stream()
                .map(place -> new PlaceSuggestion(place, query.distanceTo(place)))
                .filter(suggestion -> suggestion.distanceMeters() <= MAX_DISTANCE_METERS)
                .sorted(Comparator.comparingInt(PlaceSuggestion::distanceMeters))
                .toList();
        // OpenStreetMap often has the same place twice (the building and its entrance or bus stop): the closest stays
        var unique = new LinkedHashMap<String, PlaceSuggestion>();
        closestFirst.forEach(suggestion -> unique.putIfAbsent(
                suggestion.place().name() + "|" + suggestion.place().address(), suggestion));
        return unique.values().stream().limit(MAX_SUGGESTIONS).toList();
    }
}
