package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.PlaceQuery;
import es.upm.miw.oneleft.plans.domain.model.PlaceSuggestion;

import java.util.List;

/** Places for the meeting point while the organizer types (HU-071), closest first. */
public interface SuggestPlacesUseCase {

    List<PlaceSuggestion> suggest(PlaceQuery query);
}
