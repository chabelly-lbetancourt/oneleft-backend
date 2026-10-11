package es.upm.miw.oneleft.plans.infrastructure.places;

import es.upm.miw.oneleft.plans.domain.model.Place;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Client of Photon (free, no key). The search and the place, rounded to about 1 km, are the key of the Redis cache
 * ({@link PlacesConfig}). The circuit breaker stops calling Photon while it keeps failing; then, and on any error,
 * there are no suggestions ({@code null}, not cached).
 */
@Component
class PhotonClient {

    static final String CIRCUIT = "places";
    /** Photon weighs the distance but may still answer far away places: some more than are shown are asked for. */
    static final int RESULTS = 10;
    private static final Logger log = LoggerFactory.getLogger(PhotonClient.class);

    private final RestClient http;
    private final CircuitBreaker breaker;

    PhotonClient(RestClient photon, CircuitBreakerFactory<?, ?> breakers) {
        this.http = photon;
        this.breaker = breakers.create(CIRCUIT);
    }

    @Cacheable(cacheNames = PlacesConfig.CACHE, unless = "#result == null")
    public List<Place> search(String text, double latitude, double longitude) {
        return breaker.run(() -> request(text, latitude, longitude), error -> {
            log.warn("No place suggestions: {}", error.toString());
            return null;
        });
    }

    private List<Place> request(String text, double latitude, double longitude) {
        var response = http.get()
                .uri(uri -> uri.path("/api")
                        .queryParam("q", text)
                        .queryParam("lat", latitude)
                        .queryParam("lon", longitude)
                        .queryParam("limit", RESULTS)
                        .build())
                .retrieve()
                .body(Response.class);
        if (response == null || response.features() == null) {
            return List.of();
        }
        return response.features().stream().map(Feature::place).filter(Objects::nonNull).toList();
    }

    /** The part of the Photon answer (GeoJSON) that is used. */
    record Response(List<Feature> features) {
    }

    record Feature(Geometry geometry, Properties properties) {

        /** Null when the feature has neither a name nor a street, or no point. */
        Place place() {
            if (geometry == null || geometry.coordinates() == null || geometry.coordinates().size() < 2
                    || properties == null) {
                return null;
            }
            var street = join(" ", properties.street(), properties.housenumber());
            var name = blank(properties.name()) ? street : properties.name();
            if (blank(name)) {
                return null;
            }
            var area = blank(properties.locality()) ? properties.district() : properties.locality();
            var address = join(", ", name.equals(street) ? null : street, area, properties.city());
            // GeoJSON points are [longitude, latitude]
            return new Place(name, address, geometry.coordinates().get(1), geometry.coordinates().get(0));
        }

        private static boolean blank(String text) {
            return text == null || text.isBlank();
        }

        private static String join(String separator, String... parts) {
            return Stream.of(parts).filter(part -> !blank(part)).map(String::strip)
                    .collect(Collectors.joining(separator));
        }
    }

    record Geometry(List<Double> coordinates) {
    }

    record Properties(String name, String street, String housenumber, String locality, String district,
                      String city) {
    }
}
