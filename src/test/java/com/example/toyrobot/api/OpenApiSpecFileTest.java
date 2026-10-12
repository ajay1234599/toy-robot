package com.example.toyrobot.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Keeps the checked-in {@code openapi.yaml} identical to the spec the code generates.
 * After changing an endpoint, regenerate it with:
 * {@code mvn test -Dtest=OpenApiSpecFileTest -Dopenapi.update=true}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiSpecFileTest {

    private static final Path SPEC_FILE = Path.of("openapi.yaml");
    private static final YAMLMapper YAML = new YAMLMapper();

    @LocalServerPort
    int port;

    private String generatedSpec() throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs.yaml")).build(),
                HttpResponse.BodyHandlers.ofString()).body();
    }

    @Test
    void checkedInSpecMatchesTheCode() throws Exception {
        String generated = generatedSpec();
        if (Boolean.getBoolean("openapi.update")) {
            Files.writeString(SPEC_FILE, generated);
        }
        JsonNode expected = YAML.readTree(generated);
        JsonNode actual = YAML.readTree(Files.readString(SPEC_FILE));
        assertThat(actual)
                .as("openapi.yaml is out of date. Regenerate it with: "
                        + "mvn test -Dtest=OpenApiSpecFileTest -Dopenapi.update=true")
                .isEqualTo(expected);
    }

    @Test
    void checkedInSpecIsValidOpenApi() {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(SPEC_FILE.toString(), null, null);
        assertThat(result.getMessages()).as("validation messages").isEmpty();
        assertThat(result.getOpenAPI().getPaths()).containsKeys(
                "/robots", "/robots/{id}", "/robots/{id}/placement", "/robots/{id}/commands",
                "/robots/{id}/report", "/actuator/health", "/actuator/info");
    }
}
