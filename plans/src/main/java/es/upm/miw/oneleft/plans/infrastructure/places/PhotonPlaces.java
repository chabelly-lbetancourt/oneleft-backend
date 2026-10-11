package es.upm.miw.oneleft.plans.infrastructure.places;

import es.upm.miw.oneleft.plans.domain.model.Place;
import es.upm.miw.oneleft.plans.domain.model.PlaceQuery;
import es.upm.miw.oneleft.plans.domain.port.out.PlaceSearch;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Output adapter: places from Photon, through its cached and protected client (HU-071). */
@Component
public class PhotonPlaces implements PlaceSearch {

    private final PhotonClient client;

    public PhotonPlaces(PhotonClient client) {
        this.client = client;
    }

    /** Two decimals: about 1 km, the same answer for everyone around. */
    private static double rounded(double coordinate) {
        return Math.round(coordinate * 100) / 100.0;
    }

    /** «Pista  Pádel» and «pista pádel» are the same search, and the same entry of the cache. */
    private static String normalized(String text) {
        return text.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    @Override
    public List<Place> search(PlaceQuery query) {
        var places = client.search(normalized(query.text()), rounded(query.latitude()), rounded(query.longitude()));
        return Objects.requireNonNullElse(places, List.of());
    }
}
