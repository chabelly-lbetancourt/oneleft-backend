package es.upm.miw.oneleft.users.infrastructure.rest;

import es.upm.miw.oneleft.users.StorageContainerConfiguration;
import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.PhotoFormat;
import es.upm.miw.oneleft.users.domain.model.PhotoKey;
import es.upm.miw.oneleft.users.domain.port.in.ManagePhotoUseCase;
import es.upm.miw.oneleft.users.domain.port.out.PhotoStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-020 end to end inside the service: the photos go to a real S3-compatible storage with the signed links, as the
 * web uploads them, and PostgreSQL keeps whose they are. The photos and rows created are removed at the end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(StorageContainerConfiguration.class)
class PhotoIntegrationTest {

    private static final String BASE = UserController.USERS;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private ManagePhotoUseCase photos;

    @Autowired
    private PhotoStorage storage;

    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final byte[] photo = bytes(48_000);

    private static byte[] bytes(int size) {
        var data = new byte[size];
        Arrays.fill(data, (byte) 7);
        return data;
    }

    private static RequestPostProcessor user(UUID id) {
        return jwt().jwt(token -> token.subject(id.toString()).claim("name", "Test").claim("email", "t@oneleft.dev"));
    }

    @AfterEach
    void removePhotos() {
        for (var id : Set.of(ana, lucia)) {
            photos.removeMyPhoto(new Identity(id.toString(), "Test", "t@oneleft.dev", Set.of()));
        }
    }

    private JsonNode startUpload(UUID who, String contentType, long size) throws Exception {
        var body = mockMvc.perform(post(BASE + PhotoController.MY_PHOTO_UPLOADS).with(user(who))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\": \"" + contentType + "\", \"size\": " + size + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
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

    private String uploadAndConfirm(UUID who) throws Exception {
        var upload = startUpload(who, "image/webp", photo.length);
        assertThat(putTo(upload, "image/webp", photo)).isEqualTo(200);
        var key = upload.get("key").asString();
        mockMvc.perform(put(BASE + PhotoController.MY_PHOTO).with(user(who))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"key\": \"" + key + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(who.toString()));
        return key;
    }

    @Test
    void aPhotoGoesStraightToTheStorageAndOthersSeeItWithASignedLink() throws Exception {
        var upload = startUpload(ana, "image/webp", photo.length);
        assertThat(upload.get("method").asString()).isEqualTo("PUT");
        assertThat(upload.get("headers").get("Content-Type").asString()).isEqualTo("image/webp");
        assertThat(upload.get("key").asString()).startsWith(PhotoKey.prefixOf(ana));
        uploadAndConfirm(ana);

        var body = mockMvc.perform(get(BASE + PhotoController.PHOTOS).with(user(lucia))
                        .param("ids", ana.toString(), lucia.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(ana.toString()))
                .andReturn().getResponse().getContentAsString();
        var link = URI.create(json.readTree(body).get(0).get("url").asString());
        var seen = HTTP.send(HttpRequest.newBuilder(link).build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(seen.statusCode()).isEqualTo(200);
        assertThat(seen.body()).isEqualTo(photo);
        assertThat(seen.headers().firstValue("Content-Type")).hasValue("image/webp");

        var unsigned = URI.create(link.toString().substring(0, link.toString().indexOf('?')));
        assertThat(HTTP.send(HttpRequest.newBuilder(unsigned).build(), HttpResponse.BodyHandlers.discarding())
                .statusCode()).isEqualTo(403);
    }

    @Test
    void theSignedLinkOnlyTakesThePhotoItWasSignedFor() throws Exception {
        var upload = startUpload(ana, "image/webp", photo.length);

        assertThat(putTo(upload, "image/webp", bytes(photo.length + 1))).isEqualTo(403);
        assertThat(putTo(upload, "text/html", photo)).isEqualTo(403);

        mockMvc.perform(put(BASE + PhotoController.MY_PHOTO).with(user(ana)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\": \"" + upload.get("key").asString() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("photo.notUploaded"));
    }

    @Test
    void aNewPhotoReplacesThePreviousOneAndRemovingItDeletesIt() throws Exception {
        var first = uploadAndConfirm(ana);
        var second = uploadAndConfirm(ana);

        assertThat(storage.find(first)).isEmpty();
        assertThat(storage.find(second)).hasValueSatisfying(stored -> assertThat(stored.size()).isEqualTo(48_000));

        mockMvc.perform(delete(BASE + PhotoController.MY_PHOTO).with(user(ana))).andExpect(status().isNoContent());

        assertThat(storage.find(second)).isEmpty();
        mockMvc.perform(get(BASE + PhotoController.PHOTOS).with(user(ana)).param("ids", ana.toString()))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void eachProblemHasItsCode() throws Exception {
        var request = post(BASE + PhotoController.MY_PHOTO_UPLOADS).with(user(ana))
                .contentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(request.content("{\"contentType\": \"image/png\", \"size\": 1000}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("photo.format"));
        mockMvc.perform(post(BASE + PhotoController.MY_PHOTO_UPLOADS).with(user(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\": \"image/jpeg\", \"size\": 2000000}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("photo.tooBig"));

        var someoneElses = PhotoKey.newFor(lucia, PhotoFormat.WEBP);
        mockMvc.perform(put(BASE + PhotoController.MY_PHOTO).with(user(ana)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\": \"" + someoneElses + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("photo.key"));

        var tooMany = IntStream.range(0, 51).mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.joining(","));
        mockMvc.perform(get(BASE + PhotoController.PHOTOS).with(user(ana)).param("ids", tooMany))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("photo.tooManyPeople"));
    }

    @Test
    void photosNeedASession() throws Exception {
        mockMvc.perform(get(BASE + PhotoController.PHOTOS).param("ids", ana.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + PhotoController.MY_PHOTO_UPLOADS).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentType\": \"image/webp\", \"size\": 1000}")).andExpect(status().isUnauthorized());
    }
}
