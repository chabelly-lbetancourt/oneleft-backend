package es.upm.miw.oneleft.plans.infrastructure.places;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.infrastructure.rest.PlaceController;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-071 with Photon simulated by WireMock, the cache in a real Redis and the circuit breaker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PlaceSuggestionIntegrationTest {

    @RegisterExtension
    static final WireMockExtension PHOTON = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    private static final String API = "/api";

    /** Two points of the same sports hall, a street, a place in Valencia and a feature without a point. */
    private static final String ANSWER = """
            {"type": "FeatureCollection", "features": [
              {"type": "Feature", "geometry": {"type": "Point", "coordinates": [-0.33, 39.45]},
               "properties": {"name": "Polideportivo Nazaret", "city": "Valencia", "osm_key": "leisure"}},
              {"type": "Feature", "geometry": {"type": "Point", "coordinates": [-3.6328, 40.3841]},
               "properties": {"name": "Polideportivo Palomeras", "street": "Avenida de la Albufera",
                              "locality": "Palomeras Sureste", "district": "Puente de Vallecas", "city": "Madrid"}},
              {"type": "Feature", "geometry": {"type": "Point", "coordinates": [-3.6266, 40.3883]},
               "properties": {"name": "Polideportivo Campus Sur", "street": "Avenida del Campus Sur",
                              "district": "Puente de Vallecas", "city": "Madrid"}},
              {"type": "Feature", "geometry": {"type": "Point", "coordinates": [-3.6270, 40.3885]},
               "properties": {"name": "Polideportivo Campus Sur", "street": "Avenida del Campus Sur",
                              "district": "Puente de Vallecas", "city": "Madrid"}},
              {"type": "Feature", "geometry": {"type": "Point", "coordinates": [-3.6301, 40.3912]},
               "properties": {"street": "Calle de la Sierra de Alquife", "housenumber": "12", "city": "Madrid"}},
              {"type": "Feature", "geometry": null, "properties": {"name": "Without a point"}}
            ]}""";

    @DynamicPropertySource
    static void geocoder(DynamicPropertyRegistry registry) {
        registry.add("oneleft.places.url", PHOTON::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager caches;

    @Autowired
    private CircuitBreakerRegistry breakers;

    private static RequestPostProcessor organizer() {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("name", "Lucía"));
    }

    private ResultActions search(String query, double latitude, double longitude) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(PlaceController.PLACES).with(organizer())
                .param("query", query)
                .param("latitude", String.valueOf(latitude))
                .param("longitude", String.valueOf(longitude)));
    }

    @BeforeEach
    void reset() {
        PHOTON.resetAll();
        // invalidate, not clear: the entries of the previous test must be gone right now
        caches.getCache(PlacesConfig.CACHE).invalidate();
        breakers.circuitBreaker(PhotonClient.CIRCUIT).reset();
    }

    @Test
    void theNearbyPlacesComeClosestFirstAndTheSameSearchAroundIsAskedOnce() throws Exception {
        PHOTON.stubFor(get(urlPathEqualTo(API)).willReturn(okJson(ANSWER)));

        search("Polideportivo", 40.389, -3.628)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").value("Polideportivo Campus Sur"))
                .andExpect(jsonPath("$[0].address").value("Avenida del Campus Sur, Puente de Vallecas, Madrid"))
                .andExpect(jsonPath("$[0].latitude").value(40.3885))
                .andExpect(jsonPath("$[0].longitude").value(-3.627))
                .andExpect(jsonPath("$[0].distanceMeters").value(101))
                .andExpect(jsonPath("$[1].name").value("Calle de la Sierra de Alquife 12"))
                .andExpect(jsonPath("$[1].address").value("Madrid"))
                .andExpect(jsonPath("$[1].distanceMeters").value(302))
                .andExpect(jsonPath("$[2].name").value("Polideportivo Palomeras"))
                .andExpect(jsonPath("$[2].address")
                        .value("Avenida de la Albufera, Palomeras Sureste, Madrid"));

        // Another device in the same square kilometre, typing the same with other spaces and capitals: the same
        // places, measured from where it is
        search("  polideportivo ", 40.391, -3.631)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").value("Calle de la Sierra de Alquife 12"));

        PHOTON.verify(1, getRequestedFor(urlPathEqualTo(API))
                .withQueryParam("q", equalTo("polideportivo"))
                .withQueryParam("lat", equalTo("40.39"))
                .withQueryParam("lon", equalTo("-3.63"))
                .withQueryParam("limit", equalTo(String.valueOf(PhotonClient.RESULTS))));
    }

    @Test
    void whenTheGeocoderFailsThereAreNoSuggestionsAndTheNextSearchAsksAgain() throws Exception {
        PHOTON.stubFor(get(urlPathEqualTo(API)).willReturn(aResponse().withStatus(500)));

        search("padel", 40.39, -3.63)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        PHOTON.stubFor(get(urlPathEqualTo(API)).willReturn(okJson(ANSWER)));
        search("padel", 40.39, -3.63)
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void afterRepeatedFailuresTheCircuitOpensAndTheGeocoderIsLeftAlone() throws Exception {
        PHOTON.stubFor(get(urlPathEqualTo(API)).willReturn(aResponse().withStatus(503)));

        for (var letters = 0; letters < 8; letters++) {
            search("padel" + "s".repeat(letters), 40.39, -3.63)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        // Five failed calls open the circuit: the other three searches do not reach Photon
        PHOTON.verify(5, getRequestedFor(urlPathEqualTo(API)));
    }

    @Test
    void aSearchThatIsTooShortOrOutOfTheWorldIsRejected() throws Exception {
        search("pa", 40.39, -3.63)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("place.query"));
        search("padel", 91, -3.63)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("coordinates.outOfRange"));

        PHOTON.verify(0, getRequestedFor(urlPathEqualTo(API)));
    }

    @Test
    void onlySignedInPeopleCanSearch() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(PlaceController.PLACES)
                        .param("query", "padel").param("latitude", "40.39").param("longitude", "-3.63"))
                .andExpect(status().isUnauthorized());
    }
}
