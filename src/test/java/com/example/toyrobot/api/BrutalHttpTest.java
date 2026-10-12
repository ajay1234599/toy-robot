package com.example.toyrobot.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Black-box tests over real HTTP against the embedded Tomcat server. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrutalHttpTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String JSON_TYPE = "application/json";

    @LocalServerPort
    int port;

    record Res(int status, HttpResponse<String> raw) {
        JsonNode json() {
            try {
                return JSON.readTree(raw.body());
            } catch (Exception e) {
                throw new AssertionError("Not JSON (" + status + "): " + raw.body(), e);
            }
        }

        String header(String name) {
            return raw.headers().firstValue(name).orElse(null);
        }

        List<String> outputs() {
            List<String> out = new ArrayList<>();
            json().get("outputs").forEach(n -> out.add(n.asText()));
            return out;
        }

        /** Every error must be JSON of the form {"error": {"code", "message"}}. */
        Res isError(int expectedStatus, String code) {
            assertThat(status).as(raw.body()).isEqualTo(expectedStatus);
            assertThat(header("Content-Type")).startsWith(JSON_TYPE);
            assertThat(json().at("/error/code").asText()).isEqualTo(code);
            assertThat(json().at("/error/message").asText()).isNotBlank();
            return this;
        }
    }

    Res send(String method, String path, String contentType, String body) {
        return send(method, path, contentType, body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body));
    }

    Res send(String method, String path, String contentType, HttpRequest.BodyPublisher body) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .method(method, body);
            if (contentType != null) {
                request.header("Content-Type", contentType);
            }
            HttpResponse<String> response = HTTP.send(request.build(), BodyHandlers.ofString());
            return new Res(response.statusCode(), response);
        } catch (Exception e) {
            throw new AssertionError(method + " " + path + " failed", e);
        }
    }

    Res get(String path) {
        return send("GET", path, null, (String) null);
    }

    Res commands(String id, String jsonBody) {
        return send("POST", "/robots/" + id + "/commands", JSON_TYPE, jsonBody);
    }

    Res run(String id, String commands) {
        return commands(id, JSON.createObjectNode().put("commands", commands).toString());
    }

    Res place(String id, String jsonBody) {
        return send("PUT", "/robots/" + id + "/placement", JSON_TYPE, jsonBody);
    }

    String newRobot() {
        Res res = send("POST", "/robots", null, (String) null);
        assertThat(res.status()).isEqualTo(201);
        return res.json().get("id").asText();
    }

    String reportOf(String id) {
        return get("/robots/" + id + "/report").json().get("output").asText();
    }

    String id;

    @BeforeEach
    void freshRobot() {
        id = newRobot();
    }

    @Nested
    @DisplayName("Requirement scenarios over HTTP")
    class Requirements {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource(delimiter = '|', value = {
                "PLACE 0,0,NORTH MOVE REPORT                | 0,1,NORTH",
                "PLACE 0,0,NORTH LEFT REPORT                | 0,0,WEST",
                "PLACE 1,2,EAST MOVE MOVE LEFT MOVE REPORT  | 3,3,NORTH",
                "MOVE REPORT                                | ROBOT MISSING",
                "PLACE 0,0,SOUTH MOVE REPORT                | 0,0,SOUTH",
                "PLACE 0,0,WEST MOVE REPORT                 | 0,0,WEST",
                "PLACE 4,4,NORTH MOVE REPORT                | 4,4,NORTH",
                "PLACE 4,4,EAST MOVE REPORT                 | 4,4,EAST",
                "PLACE 5,5,NORTH MOVE REPORT                | ROBOT MISSING",
                "PLACE -1,0,NORTH REPORT                    | ROBOT MISSING",
                "PLACE 0,0,NORTH MOVE MOVE MOVE MOVE MOVE MOVE RIGHT MOVE REPORT | 1,4,EAST",
                "LEFT RIGHT MOVE PLACE 3,3,SOUTH MOVE REPORT | 3,2,SOUTH",
        })
        void scenario(String commands, String expected) {
            Res res = run(id, commands);
            assertThat(res.status()).isEqualTo(200);
            assertThat(res.outputs()).containsExactly(expected);
        }

        @Test
        void oneCommandPerRequestGivesTheSameResultAsABatch() {
            for (String c : "PLACE 1,2,EAST MOVE MOVE LEFT MOVE".split(" (?=[A-Z]{4})")) {
                assertThat(commands(id, "{\"command\":\"" + c + "\"}").status()).isEqualTo(200);
            }
            assertThat(reportOf(id)).isEqualTo("3,3,NORTH");
        }

        @Test
        void allThreeBodyShapesAreEquivalent() {
            String a = newRobot();
            String b = newRobot();
            String c = newRobot();
            assertThat(run(a, "PLACE 1,1,NORTH RIGHT MOVE REPORT").outputs()).containsExactly("2,1,EAST");
            assertThat(commands(b, "{\"commands\":[\"PLACE 1,1,NORTH\",\"RIGHT\",\"MOVE\",\"REPORT\"]}").outputs())
                    .containsExactly("2,1,EAST");
            assertThat(send("POST", "/robots/" + c + "/commands", "text/plain", "PLACE 1,1,NORTH\r\nRIGHT\nMOVE\tREPORT")
                    .outputs()).containsExactly("2,1,EAST");
        }

        @Test
        void ignoredMoveIsReportedAndDoesNotBlockFurtherMoves() {
            JsonNode results = run(id, "PLACE 0,0,SOUTH MOVE LEFT MOVE").json().get("results");
            assertThat(results.get(1).get("status").asText()).isEqualTo("IGNORED");
            assertThat(results.get(1).get("reason").asText()).isEqualTo("WOULD_FALL");
            assertThat(results.get(3).get("status").asText()).isEqualTo("APPLIED");
            assertThat(reportOf(id)).isEqualTo("1,0,EAST");
        }

        @Test
        void hugeCoordinatesInABatchAreIgnoredNotErrors() {
            Res res = run(id, "PLACE 2,2,NORTH PLACE 4294967298,2,NORTH PLACE 99999999999999999999999,0,EAST REPORT");
            assertThat(res.status()).isEqualTo(200);
            assertThat(res.json().at("/results/1/reason").asText()).isEqualTo("OFF_TABLE");
            assertThat(res.json().at("/results/2/reason").asText()).isEqualTo("OFF_TABLE");
            assertThat(res.outputs()).containsExactly("2,2,NORTH");
        }

        @Test
        void robotsAreIsolated() {
            String other = newRobot();
            run(id, "PLACE 4,4,WEST");
            run(other, "PLACE 0,0,EAST MOVE");
            assertThat(reportOf(id)).isEqualTo("4,4,WEST");
            assertThat(reportOf(other)).isEqualTo("1,0,EAST");
        }
    }

    @Nested
    @DisplayName("PUT /placement")
    class Placement {

        @ParameterizedTest(name = "accepted: {0}")
        @ValueSource(strings = {
                "{\"x\":0,\"y\":0,\"facing\":\"NORTH\"}", "{\"x\":4,\"y\":4,\"facing\":\"west\"}",
                "{\"facing\":\" South \",\"y\":2,\"x\":3}", "{\"x\":1,\"y\":1,\"facing\":\"EAST\",\"extra\":[1,2]}",
                "{\"x\":-0,\"y\":0,\"facing\":\"NORTH\"}"})
        void validPlacements(String body) {
            Res res = place(id, body);
            assertThat(res.status()).as(res.raw().body()).isEqualTo(200);
            assertThat(res.json().get("placed").asBoolean()).isTrue();
        }

        @ParameterizedTest(name = "off table: {0}")
        @ValueSource(strings = {
                "{\"x\":5,\"y\":0,\"facing\":\"NORTH\"}", "{\"x\":0,\"y\":5,\"facing\":\"NORTH\"}",
                "{\"x\":-1,\"y\":0,\"facing\":\"NORTH\"}", "{\"x\":0,\"y\":-1,\"facing\":\"NORTH\"}",
                "{\"x\":2147483648,\"y\":0,\"facing\":\"NORTH\"}", "{\"x\":4294967298,\"y\":2,\"facing\":\"NORTH\"}",
                "{\"x\":-9223372036854775809,\"y\":0,\"facing\":\"NORTH\"}",
                "{\"x\":1,\"y\":123456789012345678901234567890,\"facing\":\"NORTH\"}"})
        void offTablePlacementIs422AndChangesNothing(String body) {
            place(id, "{\"x\":2,\"y\":2,\"facing\":\"EAST\"}");
            place(id, body).isError(422, "OFF_TABLE");
            assertThat(reportOf(id)).isEqualTo("2,2,EAST");
        }

        @ParameterizedTest(name = "rejected: {0}")
        @ValueSource(strings = {
                "{}", "[]", "[1,2,\"NORTH\"]", "\"PLACE 1,1,NORTH\"", "42", "true", "null",
                "{\"y\":1,\"facing\":\"NORTH\"}", "{\"x\":1,\"facing\":\"NORTH\"}", "{\"x\":1,\"y\":1}",
                "{\"x\":\"1\",\"y\":1,\"facing\":\"NORTH\"}", "{\"x\":1.5,\"y\":1,\"facing\":\"NORTH\"}",
                "{\"x\":1.0,\"y\":1,\"facing\":\"NORTH\"}", "{\"x\":1e400,\"y\":1,\"facing\":\"NORTH\"}",
                "{\"x\":true,\"y\":1,\"facing\":\"NORTH\"}", "{\"x\":null,\"y\":1,\"facing\":\"NORTH\"}",
                "{\"x\":[1],\"y\":1,\"facing\":\"NORTH\"}", "{\"x\":1,\"y\":1,\"facing\":\"UP\"}",
                "{\"x\":1,\"y\":1,\"facing\":\"\"}", "{\"x\":1,\"y\":1,\"facing\":1}", "{\"x\":1,\"y\":1,\"facing\":null}",
                "{\"x\":1,\"y\":1,\"facing\":[\"NORTH\"]}", "{\"x\":1,\"y\":1,\"facing\":\"NORTH EAST\"}"})
        void invalidPlacementsAre400AndChangeNothing(String body) {
            place(id, "{\"x\":2,\"y\":2,\"facing\":\"EAST\"}");
            Res res = place(id, body);
            assertThat(res.status()).as(body + " -> " + res.raw().body()).isEqualTo(400);
            res.isError(400, res.json().at("/error/code").asText());
            assertThat(reportOf(id)).isEqualTo("2,2,EAST");
        }

        @ParameterizedTest(name = "malformed JSON: {0}")
        @ValueSource(strings = {"", "{", "{\"x\":1,", "{x:1,y:1,facing:NORTH}", "{'x':1}", "NaN", "{\"x\":01}"})
        void malformedJsonIs400(String body) {
            place(id, body).isError(400, "INVALID_JSON");
        }

        @Test
        void placementIsIdempotent() {
            String body = "{\"x\":3,\"y\":1,\"facing\":\"SOUTH\"}";
            String first = place(id, body).raw().body();
            String second = place(id, body).raw().body();
            assertThat(second).isEqualTo(first);
        }
    }

    @Nested
    @DisplayName("POST /commands body abuse")
    class CommandBodies {

        @ParameterizedTest(name = "200 empty: {0}")
        @ValueSource(strings = {"{\"commands\":\"\"}", "{\"commands\":\"   \"}", "{\"commands\":[]}", "[]", "\"\"",
                "{\"commands\":[\"\",\"  \"]}"})
        void emptyCommandListsDoNothing(String body) {
            Res res = commands(id, body);
            assertThat(res.status()).isEqualTo(200);
            assertThat(res.json().get("results")).isEmpty();
        }

        @ParameterizedTest(name = "400: {0}")
        @ValueSource(strings = {"{}", "null", "42", "true", "{\"commands\":null}", "{\"commands\":42}",
                "{\"commands\":{}}", "{\"commands\":[1]}", "{\"commands\":[null]}", "{\"commands\":[\"MOVE\",false]}",
                "{\"commands\":[[\"MOVE\"]]}", "{\"command\":true}", "{\"cmd\":\"MOVE\"}",
                "{\"commands\":\"PLACE 0,0,NORTH FLY\"}", "{\"commands\":\"PLACE 0,0,UP\"}",
                "{\"commands\":[\"PLACE 0,0,NORTH\",\"MOVEX\"]}"})
        void invalidBodiesAre400AndExecuteNothing(String body) {
            Res res = commands(id, body);
            assertThat(res.status()).as(body + " -> " + res.raw().body()).isEqualTo(400);
            res.isError(400, res.json().at("/error/code").asText());
            assertThat(reportOf(id)).isEqualTo("ROBOT MISSING");
        }

        @ParameterizedTest(name = "malformed JSON: [{0}]")
        @ValueSource(strings = {"", "{", "{\"commands\":", "{\"commands\":\"MOVE}", "{commands:MOVE}", "MOVE",
                "{\"commands\":\"MOVE\"}}", "{\"commands\":\"MOVE\"} {}", "{\"commands\":\"MOVE\"}[]"})
        void malformedJsonIs400(String body) {
            commands(id, body).isError(400, "INVALID_JSON");
        }

        @Test
        void utf8ByteOrderMarkIsTolerated() {
            assertThat(commands(id, "\uFEFF{\"commands\":\"PLACE 0,0,NORTH REPORT\"}").outputs())
                    .containsExactly("0,0,NORTH");
        }

        @Test
        void absurdlyNestedJsonIsRejectedNotStackOverflow() {
            commands(id, "[".repeat(100_000) + "]".repeat(100_000)).isError(400, "INVALID_JSON");
        }

        @Test
        void emptyTextBodyIs400() {
            send("POST", "/robots/" + id + "/commands", "text/plain", "").isError(400, "INVALID_JSON");
        }

        @ParameterizedTest(name = "Content-Type: {0}")
        @ValueSource(strings = {"application/xml", "application/x-www-form-urlencoded", "text/html",
                "multipart/form-data; boundary=x", "application/octet-stream", "image/png"})
        void unsupportedContentTypesAre415(String type) {
            send("POST", "/robots/" + id + "/commands", type, "MOVE").isError(415, "UNSUPPORTED_MEDIA_TYPE");
            send("PUT", "/robots/" + id + "/placement", type, "x").isError(415, "UNSUPPORTED_MEDIA_TYPE");
        }

        @Test
        void missingContentTypeIs415() {
            send("POST", "/robots/" + id + "/commands", null, "MOVE").isError(415, "UNSUPPORTED_MEDIA_TYPE");
        }

        @Test
        void charsetParametersAreHonoured() {
            assertThat(send("POST", "/robots/" + id + "/commands", "application/json; charset=UTF-8",
                    "{\"commands\":\"PLACE 1,1,NORTH REPORT\"}").outputs()).containsExactly("1,1,NORTH");
            assertThat(send("POST", "/robots/" + id + "/commands", "text/plain;charset=ISO-8859-1", "MOVE REPORT")
                    .outputs()).containsExactly("1,2,NORTH");
            assertThat(send("POST", "/robots/" + id + "/commands", "APPLICATION/JSON", "{\"command\":\"REPORT\"}")
                    .outputs()).containsExactly("1,2,NORTH");
        }
    }

    @Nested
    @DisplayName("Size limits")
    class Limits {

        @Test
        void exactlyTenThousandCommandsWork() {
            Res res = run(id, "PLACE 0,0,EAST " + "MOVE ".repeat(9_998) + "REPORT");
            assertThat(res.status()).isEqualTo(200);
            assertThat(res.outputs()).containsExactly("4,0,EAST");
        }

        @Test
        void tenThousandAndOneCommandsAre413AndExecuteNothing() {
            run(id, "PLACE " + "0,0,NORTH " + "MOVE ".repeat(10_000)).isError(413, "TOO_MANY_COMMANDS");
            send("POST", "/robots/" + id + "/commands", "text/plain", "LEFT\n".repeat(10_001))
                    .isError(413, "TOO_MANY_COMMANDS");
            assertThat(reportOf(id)).isEqualTo("ROBOT MISSING");
        }

        @Test
        void bodyOverOneMegabyteIs413() {
            String big = "{\"commands\":\"" + " ".repeat(RequestSizeLimitFilter.MAX_BODY_BYTES) + "\"}";
            commands(id, big).isError(413, "PAYLOAD_TOO_LARGE");
        }

        @Test
        void chunkedBodyOverOneMegabyteIs413() {
            byte[] big = ("{\"commands\":\"" + " ".repeat(2 * RequestSizeLimitFilter.MAX_BODY_BYTES) + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            Res res = send("POST", "/robots/" + id + "/commands", JSON_TYPE,
                    BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(big)));
            res.isError(413, "PAYLOAD_TOO_LARGE");
            Res text = send("POST", "/robots/" + id + "/commands", "text/plain",
                    BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(big)));
            text.isError(413, "PAYLOAD_TOO_LARGE");
        }
    }

    @Nested
    @DisplayName("Routing, methods and unknown robots")
    class Routing {

        @ParameterizedTest(name = "{0} {1} -> 404")
        @CsvSource({"GET,/robots/nope", "DELETE,/robots/nope", "GET,/robots/nope/report",
                "PUT,/robots/nope/placement", "POST,/robots/nope/commands",
                "GET,/robots/00000000-0000-0000-0000-000000000000"})
        void unknownRobotIs404(String method, String path) {
            String body = method.equals("PUT") ? "{\"x\":0,\"y\":0,\"facing\":\"NORTH\"}" : "{\"command\":\"MOVE\"}";
            send(method, path, method.matches("PUT|POST") ? JSON_TYPE : null, method.matches("PUT|POST") ? body : null)
                    .isError(404, "ROBOT_NOT_FOUND");
        }

        @ParameterizedTest(name = "GET {0} -> 404")
        @ValueSource(strings = {"/", "/robot", "/robots/x/y/z", "/api/robots", "/toy-robot/robots", "/ROBOTS",
                "/robots/", "/robots//report", "/health", "/actuator/env", "/actuator/beans"})
        void unknownRoutesAre404Json(String path) {
            assertThat(get(path).status()).isEqualTo(404);
            assertThat(get(path).header("Content-Type")).startsWith(JSON_TYPE);
        }

        @ParameterizedTest(name = "{0} {1} -> 405, Allow: {2}")
        @CsvSource(delimiter = '|', value = {
                "PUT    | /robots                | GET,POST",
                "DELETE | /robots                | GET,POST",
                "PATCH  | /robots                | GET,POST",
                "POST   | /robots/ID             | GET,DELETE",
                "PUT    | /robots/ID             | GET,DELETE",
                "PATCH  | /robots/ID             | GET,DELETE",
                "GET    | /robots/ID/placement   | PUT",
                "POST   | /robots/ID/placement   | PUT",
                "DELETE | /robots/ID/placement   | PUT",
                "GET    | /robots/ID/commands    | POST",
                "PUT    | /robots/ID/commands    | POST",
                "DELETE | /robots/ID/commands    | POST",
                "POST   | /robots/ID/report      | GET",
                "PUT    | /robots/ID/report      | GET",
                "POST   | /actuator/health       | GET",
        })
        void wrongMethodIs405WithAllowHeader(String method, String path, String allowed) {
            Res res = send(method, path.replace("ID", id), null, (String) null).isError(405, "METHOD_NOT_ALLOWED");
            Set<String> allow = new HashSet<>(List.of(res.header("Allow").replace(" ", "").split(",")));
            assertThat(allow).containsAll(List.of(allowed.split(",")));
        }

        @Test
        void deletedRobotIsGoneEverywhere() {
            run(id, "PLACE 1,1,NORTH");
            assertThat(send("DELETE", "/robots/" + id, null, (String) null).status()).isEqualTo(204);
            get("/robots/" + id).isError(404, "ROBOT_NOT_FOUND");
            get("/robots/" + id + "/report").isError(404, "ROBOT_NOT_FOUND");
            run(id, "MOVE").isError(404, "ROBOT_NOT_FOUND");
            place(id, "{\"x\":0,\"y\":0,\"facing\":\"NORTH\"}").isError(404, "ROBOT_NOT_FOUND");
            send("DELETE", "/robots/" + id, null, (String) null).isError(404, "ROBOT_NOT_FOUND");
            assertThat(get("/robots").json().get("robots").findValuesAsText("id")).doesNotContain(id);
        }

        @Test
        void createIgnoresAnyBodyAndReturnsLocation() {
            Res res = send("POST", "/robots", JSON_TYPE, "{\"x\":3}");
            assertThat(res.status()).isEqualTo(201);
            assertThat(res.header("Location")).isEqualTo("/robots/" + res.json().get("id").asText());
            assertThat(res.json().get("placed").asBoolean()).isFalse();
        }

        @Test
        void healthAndHeadAndOptions() {
            assertThat(get("/actuator/health").json().get("status").asText()).isEqualTo("UP");
            assertThat(get("/actuator/health/liveness").json().get("status").asText()).isEqualTo("UP");
            assertThat(get("/actuator/health/readiness").json().get("status").asText()).isEqualTo("UP");
            assertThat(send("HEAD", "/robots/" + id, null, (String) null).status()).isEqualTo(200);
            assertThat(send("OPTIONS", "/robots/" + id, null, (String) null).header("Allow")).contains("GET");
        }

        @Test
        void unacceptableAcceptHeaderIsNot500() throws Exception {
            HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/robots/" + id))
                    .header("Accept", "application/xml").build(), BodyHandlers.ofString());
            assertThat(res.statusCode()).isEqualTo(406);
        }

        @Test
        void responseNeverLeaksStackTraces() {
            for (Res res : List.of(run(id, "BOOM"), place(id, "{"), get("/nope"), get("/robots/nope"))) {
                assertThat(res.raw().body()).doesNotContain("Exception", "at com.", "at org.", "trace");
            }
        }
    }

    @Nested
    @DisplayName("Concurrency")
    class Concurrency {

        private <T> List<T> parallel(int tasks, java.util.function.IntFunction<T> task) throws Exception {
            try (ExecutorService pool = Executors.newFixedThreadPool(32)) {
                List<CompletableFuture<T>> futures = new ArrayList<>();
                for (int i = 0; i < tasks; i++) {
                    int n = i;
                    futures.add(CompletableFuture.supplyAsync(() -> task.apply(n), pool));
                }
                List<T> results = new ArrayList<>();
                for (CompletableFuture<T> f : futures) {
                    results.add(f.get());
                }
                return results;
            }
        }

        @Test
        void fiftyConcurrentMovesNorthStopExactlyAtTheEdge() throws Exception {
            run(id, "PLACE 2,0,NORTH");
            List<Res> results = parallel(50, n -> run(id, "MOVE"));
            assertThat(results).allMatch(r -> r.status() == 200);
            long applied = results.stream().filter(r -> r.json().at("/results/0/status").asText().equals("APPLIED")).count();
            assertThat(applied).isEqualTo(4);
            assertThat(reportOf(id)).isEqualTo("2,4,NORTH");
        }

        @Test
        void fourHundredConcurrentRightTurnsLoseNoUpdates() throws Exception {
            run(id, "PLACE 1,1,NORTH");
            parallel(400, n -> run(id, "RIGHT"));
            assertThat(reportOf(id)).isEqualTo("1,1,NORTH");
            parallel(201, n -> run(id, "LEFT"));
            assertThat(reportOf(id)).isEqualTo("1,1,WEST");
        }

        @Test
        void concurrentBatchesAreAtomic() throws Exception {
            run(id, "PLACE 0,0,NORTH");
            // Each batch is a closed loop. If batches interleaved, a REPORT could land mid-loop.
            List<Res> results = parallel(200, n -> run(id, "MOVE RIGHT MOVE RIGHT MOVE RIGHT MOVE RIGHT REPORT"));
            assertThat(results).allMatch(r -> r.status() == 200 && r.outputs().equals(List.of("0,0,NORTH")));
        }

        @Test
        void randomConcurrentTrafficNeverLeavesTheTable() throws Exception {
            String[] options = {"MOVE", "MOVE MOVE MOVE", "LEFT", "RIGHT", "REPORT", "PLACE 4,4,NORTH",
                    "PLACE 0,0,SOUTH", "PLACE 9,9,EAST", "MOVE MOVE MOVE MOVE MOVE MOVE"};
            AtomicInteger seed = new AtomicInteger();
            List<Res> results = parallel(1_000, n -> {
                Random r = new Random(seed.incrementAndGet());
                return run(id, options[r.nextInt(options.length)] + " REPORT");
            });
            for (Res res : results) {
                assertThat(res.status()).isEqualTo(200);
                JsonNode position = res.json().at("/robot/position");
                if (!position.isNull() && !position.isMissingNode()) {
                    assertThat(position.get("x").asInt()).isBetween(0, 4);
                    assertThat(position.get("y").asInt()).isBetween(0, 4);
                }
            }
        }

        @Test
        void concurrentCreatesProduceUniqueRobots() throws Exception {
            List<String> ids = parallel(500, n -> newRobot());
            assertThat(new HashSet<>(ids)).hasSize(500);
            assertThat(get("/robots").json().get("robots").findValuesAsText("id")).containsAll(ids);
        }

        @Test
        void concurrentDeleteAndCommandsNever500() throws Exception {
            List<String> ids = Collections.synchronizedList(new ArrayList<>());
            for (int i = 0; i < 50; i++) {
                ids.add(newRobot());
            }
            List<Integer> statuses = parallel(500, n -> {
                String target = ids.get(n % ids.size());
                return n % 5 == 0 ? send("DELETE", "/robots/" + target, null, (String) null).status()
                        : run(target, "PLACE 1,1,NORTH MOVE REPORT").status();
            });
            assertThat(statuses).allMatch(s -> s == 200 || s == 204 || s == 404);
        }
    }

    @Nested
    @DisplayName("Fuzz: random garbage never causes a 5xx")
    class Fuzz {

        @Test
        void twoThousandRandomRequestsNeverFail() {
            Random random = new Random(42);
            String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH"};
            String[] paths = {"/robots", "/robots/ID", "/robots/ID/commands", "/robots/ID/placement",
                    "/robots/ID/report", "/robots/nope/commands"};
            String[] types = {JSON_TYPE, "text/plain", null, "application/xml"};
            String alphabet = "PLACEMOVLFTRIGHNSUWX0123456789-,{}[]\":. \n\t\\\u0000é中";
            for (int i = 0; i < 2_000; i++) {
                StringBuilder body = new StringBuilder();
                int length = random.nextInt(60);
                for (int j = 0; j < length; j++) {
                    body.append(alphabet.charAt(random.nextInt(alphabet.length())));
                }
                String payload = random.nextBoolean() ? body.toString()
                        : "{\"commands\":" + JSON.valueToTree(body.toString()) + "}";
                String method = methods[random.nextInt(methods.length)];
                String path = paths[random.nextInt(paths.length)].replace("ID", id);
                Res res = send(method, path, types[random.nextInt(types.length)], payload);
                assertThat(res.status()).as("%s %s %s", method, path, payload).isBetween(200, 499);
                if (res.status() >= 400) {
                    res.isError(res.status(), res.json().at("/error/code").asText());
                }
                if (res.status() == 204) {
                    id = newRobot();
                }
            }
            for (JsonNode robot : get("/robots").json().get("robots")) {
                JsonNode p = robot.get("position");
                if (!p.isNull()) {
                    assertThat(Map.of("x", p.get("x").asInt(), "y", p.get("y").asInt()))
                            .allSatisfy((k, v) -> assertThat(v).isBetween(0, 4));
                }
            }
        }
    }
}
