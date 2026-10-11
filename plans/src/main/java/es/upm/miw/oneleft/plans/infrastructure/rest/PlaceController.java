package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.PlaceQuery;
import es.upm.miw.oneleft.plans.domain.model.PlaceSuggestion;
import es.upm.miw.oneleft.plans.domain.port.in.SuggestPlacesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(PlaceController.PLACES)
@Tag(name = "Places", description = "Places for the meeting point of a plan")
public class PlaceController {

    public static final String PLACES = "/api/v1/places";

    private final SuggestPlacesUseCase places;

    public PlaceController(SuggestPlacesUseCase places) {
        this.places = places;
    }

    @GetMapping
    @Operation(summary = "Suggest places for the meeting point (HU-071)",
            description = "Places that match what is typed, closest first, from an OpenStreetMap geocoder (Photon). "
                    + "Empty when nothing matches nearby or the geocoder does not answer.")
    @ApiResponse(responseCode = "200", description = "Up to 5 places within 50 km")
    @ApiResponse(responseCode = "400", description = "place.query, coordinates.outOfRange", content = @Content)
    public List<PlaceResponse> suggest(
            @Parameter(description = "What is typed, 3 to 100 characters", example = "polideportivo")
            @RequestParam String query,
            @Parameter(description = "Approximate latitude of the device", example = "40.39")
            @RequestParam double latitude,
            @Parameter(description = "Approximate longitude of the device", example = "-3.63")
            @RequestParam double longitude) {
        return places.suggest(new PlaceQuery(query, latitude, longitude)).stream().map(PlaceResponse::of).toList();
    }

    @Schema(description = "A place for the meeting point")
    public record PlaceResponse(@Schema(example = "Polideportivo Campus Sur") String name,
                                @Schema(example = "Avenida del Campus Sur, Palomeras Sureste, Madrid")
                                String address,
                                @Schema(example = "40.3885") double latitude,
                                @Schema(example = "-3.6266") double longitude,
                                @Schema(description = "Metres from the device", example = "350")
                                int distanceMeters) {

        static PlaceResponse of(PlaceSuggestion suggestion) {
            var place = suggestion.place();
            return new PlaceResponse(place.name(), place.address(), place.latitude(), place.longitude(),
                    suggestion.distanceMeters());
        }
    }
}
