package com.example.toyrobot.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiDocsTest {

    /** Every endpoint and the status codes its docs must list. */
    private static final Map<String, Map<String, String[]>> EXPECTED = Map.of(
            "/robots", Map.of(
                    "post", new String[] {"201"},
                    "get", new String[] {"200"}),
            "/robots/{id}", Map.of(
                    "get", new String[] {"200", "404"},
                    "delete", new String[] {"204", "404"}),
            "/robots/{id}/placement", Map.of(
                    "put", new String[] {"200", "400", "404", "415", "422"}),
            "/robots/{id}/report", Map.of(
                    "get", new String[] {"200", "404"}),
            "/robots/{id}/commands", Map.of(
                    "post", new String[] {"200", "400", "404", "413", "415"}));

    @LocalServerPort
    int port;

    JsonNode spec;

    @BeforeEach
    void loadSpec() throws Exception {
        spec = new ObjectMapper().readTree(get("/v3/api-docs").body());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void specDescribesTheApi() {
        assertThat(spec.at("/info/title").asText()).isEqualTo("Toy Robot API");
        assertThat(spec.at("/openapi").asText()).startsWith("3.");
        assertThat(spec.get("paths").properties()).extracting(Map.Entry::getKey)
                .containsAll(EXPECTED.keySet())
                .contains("/actuator/health", "/actuator/info");
        assertThat(spec.at("/servers/0/url").asText()).isEqualTo("http://localhost:8000");
    }

    @Test
    void everyEndpointHasASummaryDescriptionAndEveryStatusCode() {
        EXPECTED.forEach((path, methods) -> methods.forEach((method, codes) -> {
            JsonNode operation = spec.at("/paths/" + path.replace("/", "~1") + "/" + method);
            assertThat(operation.isMissingNode()).as("%s %s", method, path).isFalse();
            assertThat(operation.get("summary").asText()).as("%s %s summary", method, path).isNotBlank();
            assertThat(operation.get("description").asText()).as("%s %s description", method, path).isNotBlank();
            assertThat(operation.get("responses").properties()).extracting(Map.Entry::getKey)
                    .as("%s %s responses", method, path).containsExactlyInAnyOrder(codes);
        }));
    }

    @Test
    void everyJsonResponseHasAnExample() {
        EXPECTED.forEach((path, methods) -> methods.forEach((method, codes) -> {
            JsonNode responses = spec.at("/paths/" + path.replace("/", "~1") + "/" + method + "/responses");
            for (String code : codes) {
                JsonNode json = responses.at("/" + code + "/content/application~1json");
                if (code.equals("204")) {
                    assertThat(json.isMissingNode()).isTrue();
                } else {
                    assertThat(hasExample(json)).as("%s %s %s", method, path, code).isTrue();
                }
            }
        }));
    }

    @Test
    void requestBodiesAreDocumentedWithExamples() {
        JsonNode placement = spec.at("/paths/~1robots~1{id}~1placement/put/requestBody/content/application~1json");
        assertThat(hasExample(placement)).isTrue();

        JsonNode commands = spec.at("/paths/~1robots~1{id}~1commands/post/requestBody/content");
        assertThat(commands.at("/application~1json/examples")).hasSize(3);
        assertThat(commands.at("/text~1plain/examples")).hasSize(1);
    }

    /** A single example is written as {@code example}, several as named {@code examples}. */
    private static boolean hasExample(JsonNode content) {
        return content.hasNonNull("example") || (content.has("examples") && !content.get("examples").isEmpty());
    }

    @Test
    void jsonExamplesAreObjectsNotStrings() {
        // springdoc parses each example string; one with broken JSON would come out as a plain string.
        for (JsonNode content : spec.findValues("application/json")) {
            if (content.has("example")) {
                assertThat(content.get("example").isObject()).as(content.toString()).isTrue();
            }
            content.path("examples").forEach(e -> assertThat(e.get("value").isObject()).as(e.toString()).isTrue());
        }
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        HttpResponse<String> ui = get("/swagger-ui.html");
        assertThat(ui.statusCode()).isEqualTo(200);
        assertThat(ui.body()).containsIgnoringCase("swagger");
    }
}
