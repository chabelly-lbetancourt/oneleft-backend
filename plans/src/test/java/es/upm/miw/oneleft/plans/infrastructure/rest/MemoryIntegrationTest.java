package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.StorageContainerConfiguration;
import es.upm.miw.oneleft.plans.TestcontainersConfiguration;
import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Memory;
import es.upm.miw.oneleft.plans.domain.model.MemoryKey;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.port.out.MemoryStorage;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import es.upm.miw.oneleft.plans.infrastructure.messaging.AccountDeletedListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-052 end to end inside the service: the photos go to a real S3-compatible storage with the signed links, as the
 * web uploads them, and PostgreSQL keeps whose they are. Only the group of the plan reaches the album. The plans,
 * rows and photos created are removed at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, StorageContainerConfiguration.class})
class MemoryIntegrationTest {

    private static final String UPLOADS = PlanController.PLANS + MemoryController.MEMORY_UPLOADS;
    private static final String ALBUM = PlanController.PLANS + MemoryController.MEMORIES;
    private static final String PHOTO = PlanController.PLANS + MemoryController.MEMORY;
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final int SIZE = 48_000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private PlanRepository plans;

    @Autowired
    private MemoryStorage storage;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AccountDeletedListener accountDeleted;

    private final List<UUID> created = new ArrayList<>();
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final UUID marta = UUID.randomUUID();

    /** A real WebP starts with RIFF, its size and WEBP */
    private static byte[] webp() {
        return file("RIFFø»\u0000\u0000WEBPVP8 ");
    }

    private static byte[] file(String start) {
        var data = new byte[SIZE];
        Arrays.fill(data, (byte) 7);
        var head = start.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(head, 0, data, 0, head.length);
        return data;
    }

    private static RequestPostProcessor user(UUID id) {
        return jwt().jwt(token -> token.subject(id.toString()).claim("name", "Test"));
    }

    @AfterEach
    void removePlansAndPhotos() {
        // The rows of the album go with the plan (ON DELETE CASCADE)
        created.forEach(id -> jdbc.update("DELETE FROM plan WHERE id = ?", id));
        for (var id : List.of(ana, lucia, marta)) {
            storage.deleteUnder(MemoryKey.prefixOf(id));
        }
    }

    /** A plan of Ana with Lucía in it, that started that long ago (it is over 3 hours after its start) */
    private Plan planThatStarted(Duration ago) {
        var then = Clock.fixed(Instant.now().minus(ago).minus(Duration.ofMinutes(10)), ZoneOffset.UTC);
        var plan = plans.save(Plan.publish(new Organizer(ana, "Ana"), Activity.PADEL, "Padel 2 vs 2", null,
                new MeetingPoint("Courts", 40.39, -3.62), then.instant().plus(Duration.ofMinutes(10)), 3, null, then)
                .join(lucia, "Lucía", then));
        created.add(plan.id());
        return plan;
    }

    private Plan planOver() {
        return planThatStarted(Duration.ofHours(4));
    }

    private ResultActions startUpload(Plan plan, UUID who, String contentType, long size) throws Exception {
        return mockMvc.perform(post(UPLOADS, plan.id()).with(user(who)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentType\": \"" + contentType + "\", \"size\": " + size + "}"));
    }

    private ResultActions confirm(Plan plan, UUID who, String key) throws Exception {
        return mockMvc.perform(post(ALBUM, plan.id()).with(user(who)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\": \"" + key + "\"}"));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** As the browser does: a PUT with the Content-Type of the link (the Content-Length goes by itself) */
    private static int putTo(JsonNode upload, String contentType, byte[] data)
            throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(upload.get("url").asString()))
                .header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    /** Uploads to the storage what the link was signed for and returns its key */
    private String upload(Plan plan, UUID who, byte[] data) throws Exception {
        var upload = body(startUpload(plan, who, "image/webp", data.length).andExpect(status().isCreated()));
        assertThat(putTo(upload, "image/webp", data)).isEqualTo(200);
        return upload.get("key").asString();
    }

    private JsonNode add(Plan plan, UUID who) throws Exception {
        return body(confirm(plan, who, upload(plan, who, webp())).andExpect(status().isCreated()));
    }

    /** Rows of the album without a photo behind, to reach the limits without uploading them all */
    private void addRows(Plan plan, UUID who, int count) {
        for (int i = 0; i < count; i++) {
            jdbc.update("INSERT INTO plan_memory (id, plan_id, user_id, object_key, size_bytes, uploaded_at) "
                            + "VALUES (?, ?, ?, ?, 1000, ?)", UUID.randomUUID(), plan.id(), who,
                    MemoryKey.prefixOf(who, plan.id()) + UUID.randomUUID() + ".webp",
                    Timestamp.from(Instant.now().minusSeconds(i)));
        }
    }

    @Test
    void aPhotoGoesStraightToTheStorageAndTheGroupSeesItWithASignedLink() throws Exception {
        var plan = planOver();
        var upload = body(startUpload(plan, lucia, "image/webp", SIZE).andExpect(status().isCreated()));
        assertThat(upload.get("method").asString()).isEqualTo("PUT");
        assertThat(upload.get("headers").get("Content-Type").asString()).isEqualTo("image/webp");
        assertThat(upload.get("key").asString()).startsWith(MemoryKey.prefixOf(lucia, plan.id())).endsWith(".webp");
        assertThat(putTo(upload, "image/webp", webp())).isEqualTo(200);

        var added = confirm(plan, lucia, upload.get("key").asString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planId").value(plan.id().toString()))
                .andExpect(jsonPath("$.userId").value(lucia.toString()))
                .andExpect(jsonPath("$.size").value(SIZE))
                .andReturn().getResponse().getContentAsString();
        // Adding it again changes nothing
        confirm(plan, lucia, upload.get("key").asString()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(json.readTree(added).get("id").asString()));

        var album = body(mockMvc.perform(get(ALBUM, plan.id()).with(user(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[0].userId").value(lucia.toString())));
        var link = URI.create(album.get("items").get(0).get("url").asString());
        var seen = HTTP.send(HttpRequest.newBuilder(link).build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(seen.statusCode()).isEqualTo(200);
        assertThat(seen.body()).isEqualTo(webp());

        var unsigned = URI.create(link.toString().substring(0, link.toString().indexOf('?')));
        assertThat(HTTP.send(HttpRequest.newBuilder(unsigned).build(), HttpResponse.BodyHandlers.discarding())
                .statusCode()).isEqualTo(403);
    }

    @Test
    void theAlbumNeedsASession() throws Exception {
        var plan = planOver();

        mockMvc.perform(get(ALBUM, plan.id())).andExpect(status().isUnauthorized());
        mockMvc.perform(post(UPLOADS, plan.id()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentType\": \"image/webp\", \"size\": 1000}")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ALBUM, plan.id()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\": \"any\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(PHOTO, plan.id(), UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void whoDidNotTakePartNeitherSeesNorUploadsNorRemoves() throws Exception {
        var plan = planOver();
        var photo = add(plan, lucia);

        mockMvc.perform(get(ALBUM, plan.id()).with(user(marta)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("plan.notParticipant"));
        startUpload(plan, marta, "image/webp", SIZE)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("plan.notParticipant"));
        confirm(plan, marta, MemoryKey.prefixOf(marta, plan.id()) + "photo.webp")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("plan.notParticipant"));
        mockMvc.perform(delete(PHOTO, plan.id(), photo.get("id").asString()).with(user(marta)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("plan.notParticipant"));

        mockMvc.perform(get(ALBUM, UUID.randomUUID()).with(user(lucia)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("plan.notFound"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_memory WHERE plan_id = ?", Long.class, plan.id()))
                .isEqualTo(1);
    }

    @Test
    void aFileThatOnlySaysItIsAPhotoIsRejectedAndDeleted() throws Exception {
        var plan = planOver();
        var key = upload(plan, lucia, file("<html><script>alert(1)</script>"));

        confirm(plan, lucia, key)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("memory.notAPhoto"));

        assertThat(storage.find(key)).isEmpty();
        mockMvc.perform(get(ALBUM, plan.id()).with(user(lucia))).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void theSignedLinkOnlyTakesWhatItWasSignedFor() throws Exception {
        var plan = planOver();
        var upload = body(startUpload(plan, lucia, "image/webp", SIZE).andExpect(status().isCreated()));

        assertThat(putTo(upload, "image/webp", Arrays.copyOf(webp(), SIZE + 1))).isEqualTo(403);
        assertThat(putTo(upload, "text/html", webp())).isEqualTo(403);

        confirm(plan, lucia, upload.get("key").asString())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("memory.notUploaded"));
    }

    @Test
    void eachProblemOfAnUploadHasItsCode() throws Exception {
        var plan = planOver();

        startUpload(plan, lucia, "image/png", 1000)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("memory.format"));
        startUpload(plan, lucia, "image/jpeg", 3_000_000)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("memory.tooBig"));
        confirm(plan, lucia, MemoryKey.prefixOf(ana, plan.id()) + "photo.webp")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("memory.key"));

        var notOver = planThatStarted(Duration.ofHours(1));
        startUpload(notOver, lucia, "image/webp", 1000)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("memory.planNotFinished"));
        mockMvc.perform(get(ALBUM, notOver.id()).with(user(lucia)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void theAlbumAndEachPersonHaveALimitAndTheAlbumComesInPages() throws Exception {
        var plan = planOver();
        addRows(plan, lucia, Memory.MAX_PER_PERSON);
        var key = upload(plan, ana, webp());

        startUpload(plan, lucia, "image/webp", SIZE)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("memory.personLimit"));
        mockMvc.perform(get(ALBUM, plan.id()).with(user(ana)).param("page", "1").param("size", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.total").value(Memory.MAX_PER_PERSON));
        mockMvc.perform(get(ALBUM, plan.id()).with(user(ana)).param("size", "51"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("page.invalid"));

        addRows(plan, marta, Memory.MAX_PER_PLAN - Memory.MAX_PER_PERSON);
        startUpload(plan, ana, "image/webp", SIZE)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("memory.planLimit"));
        // Also for a photo uploaded while there was room
        confirm(plan, ana, key)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("memory.planLimit"));
    }

    @Test
    void eachOneRemovesOnlyTheirOwnPhotos() throws Exception {
        var plan = planOver();
        var photo = add(plan, lucia);
        var id = photo.get("id").asString();
        var key = jdbc.queryForObject("SELECT object_key FROM plan_memory WHERE id = ?", String.class,
                UUID.fromString(id));

        mockMvc.perform(delete(PHOTO, plan.id(), id).with(user(ana)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("memory.notYours"));
        assertThat(storage.find(key)).isPresent();

        mockMvc.perform(delete(PHOTO, plan.id(), id).with(user(lucia))).andExpect(status().isNoContent());

        assertThat(storage.find(key)).isEmpty();
        mockMvc.perform(delete(PHOTO, plan.id(), id).with(user(lucia)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("memory.notFound"));
    }

    @Test
    void thePhotosAreInMyDataAndGoWithMyAccount() throws Exception {
        var plan = planOver();
        add(plan, lucia);
        var ofAna = add(plan, ana);
        var key = jdbc.queryForObject("SELECT object_key FROM plan_memory WHERE user_id = ?", String.class, lucia);

        mockMvc.perform(get(MyDataController.MY_DATA).with(user(lucia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memories", hasSize(1)))
                .andExpect(jsonPath("$.memories[0].plan_id").value(plan.id().toString()))
                .andExpect(jsonPath("$.memories[0].object_key").value(key));

        accountDeleted.onDeleted(new AccountDeletedListener.AccountDeleted(lucia, Instant.now()));
        // The event can arrive twice
        accountDeleted.onDeleted(new AccountDeletedListener.AccountDeleted(lucia, Instant.now()));

        assertThat(storage.find(key)).isEmpty();
        mockMvc.perform(get(ALBUM, plan.id()).with(user(ana)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(ofAna.get("id").asString()));
    }
}
