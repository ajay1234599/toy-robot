package com.example.toyrobot.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Checks that a single request is logged by every layer, all tagged with its request id. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "logging.level.com.example.toyrobot=DEBUG")
@ExtendWith(OutputCaptureExtension.class)
class LoggingTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    int port;

    private HttpResponse<String> send(String method, String path, String requestId, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        if (requestId != null) {
            request.header("X-Request-Id", requestId);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String robotId(HttpResponse<String> created) {
        return created.body().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    /** Lines logged for one request id. */
    private static String linesFor(CapturedOutput output, String requestId) {
        StringBuilder lines = new StringBuilder();
        for (String line : output.getAll().split("\\R")) {
            if (line.contains("[" + requestId + "]")) {
                lines.append(line).append('\n');
            }
        }
        return lines.toString();
    }

    @Test
    void oneRequestIsLoggedByEveryLayerWithTheSameRequestId(CapturedOutput output) throws Exception {
        String id = robotId(send("POST", "/robots", "setup-1", null));
        HttpResponse<String> res = send("POST", "/robots/" + id + "/commands", "trace-42",
                "{\"commands\":\"PLACE 0,4,NORTH MOVE RIGHT MOVE REPORT\"}");

        assertThat(res.headers().firstValue("X-Request-Id")).contains("trace-42");
        String lines = linesFor(output, "trace-42");
        assertThat(lines)
                .contains("RequestLoggingFilter", "--> POST /robots/" + id + "/commands")          // web layer
                .contains("RobotRepository", "Found robot " + id)                                  // repository
                .contains("CommandParser", "Parsed 5 commands")                                    // parser
                .contains("RobotController", "received 5 commands (json)")                         // controller
                .contains(".Robot ", "executing 5 commands from not placed")                       // robot aggregate
                .contains("Simulator", "MOVE ignored (WOULD_FALL), robot stays at 0,4,NORTH")      // simulation
                .contains("MOVE applied: 0,4,EAST -> 1,4,EAST")
                .contains("5 commands, 4 applied, 1 ignored, now 1,4,EAST")                       // controller result
                .contains("<-- POST /robots/" + id + "/commands 200");                             // web layer
    }

    @Test
    void lifecycleEventsAreLogged(CapturedOutput output) throws Exception {
        String id = robotId(send("POST", "/robots", "life-1", null));
        send("PUT", "/robots/" + id + "/placement", "life-2", "{\"x\":1,\"y\":2,\"facing\":\"EAST\"}");
        send("PUT", "/robots/" + id + "/placement", "life-3", "{\"x\":9,\"y\":2,\"facing\":\"EAST\"}");
        send("GET", "/robots/" + id + "/report", "life-4", null);
        send("DELETE", "/robots/" + id, "life-5", null);

        assertThat(linesFor(output, "life-1")).contains("Created robot " + id, "Stored robot " + id, "<-- POST /robots 201");
        assertThat(linesFor(output, "life-2")).contains("placed at 1,2,EAST");
        assertThat(linesFor(output, "life-3")).contains("PLACE 9,2,EAST rejected", "WARN", "422 OFF_TABLE");
        assertThat(linesFor(output, "life-4")).contains("REPORT -> 1,2,EAST");
        assertThat(linesFor(output, "life-5")).contains("Removed robot " + id, "Deleted robot " + id, "204");
    }

    @Test
    void clientErrorsAreLoggedAsWarnings(CapturedOutput output) throws Exception {
        String id = robotId(send("POST", "/robots", "err-0", null));
        send("POST", "/robots/" + id + "/commands", "err-1", "{\"commands\":\"PLACE 0,0,NORTH JUMP\"}");
        send("GET", "/robots/missing", "err-2", null);

        assertThat(linesFor(output, "err-1")).contains("Rejected command token \"JUMP\"", "WARN", "400 INVALID_COMMAND");
        assertThat(linesFor(output, "err-2")).contains("No robot missing in store", "WARN", "404 ROBOT_NOT_FOUND");
    }

    @Test
    void unsafeRequestIdsAreReplacedSoTheyCantForgeLogLines(CapturedOutput output) throws Exception {
        HttpResponse<String> res = send("GET", "/robots", "bad id with spaces and ]brackets[", null);
        String issued = res.headers().firstValue("X-Request-Id").orElseThrow();
        assertThat(issued).matches("[0-9a-f-]{36}");
        assertThat(linesFor(output, issued)).contains("--> GET /robots");
        assertThat(output.getAll()).doesNotContain("]brackets[");
    }

    @Test
    void requestsWithoutAnIdGetOne() throws Exception {
        assertThat(send("GET", "/robots", null, null).headers().firstValue("X-Request-Id")).isPresent();
    }
}
