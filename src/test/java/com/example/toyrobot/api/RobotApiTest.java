package com.example.toyrobot.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class RobotApiTest {

    @Autowired
    private MockMvc mvc;

    private String newRobot() throws Exception {
        String body = mvc.perform(post("/robots"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String body) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void createReturnsLocationAndUnplacedRobot() throws Exception {
        mvc.perform(post("/robots"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/robots/")))
                .andExpect(jsonPath("$.placed").value(false))
                .andExpect(jsonPath("$.position").isEmpty())
                .andExpect(jsonPath("$.report").value("ROBOT MISSING"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "PLACE 0,0,NORTH MOVE REPORT                | 0,1,NORTH",
            "PLACE 0,0,NORTH LEFT REPORT                | 0,0,WEST",
            "PLACE 1,2,EAST MOVE MOVE LEFT MOVE REPORT  | 3,3,NORTH",
            "MOVE REPORT                                | ROBOT MISSING",
    })
    void specificationExamples(String commands, String expected) throws Exception {
        String id = newRobot();
        json(post("/robots/{id}/commands", id), "{\"commands\":\"" + commands + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outputs", contains(expected)));
    }

    @Test
    void commandsAcceptJsonArrayAndPlainText() throws Exception {
        String id = newRobot();
        json(post("/robots/{id}/commands", id), "{\"commands\":[\"PLACE 1,1,NORTH\",\"RIGHT\",\"MOVE\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.robot.position.x").value(2))
                .andExpect(jsonPath("$.robot.position.y").value(1))
                .andExpect(jsonPath("$.robot.position.facing").value("EAST"));

        mvc.perform(post("/robots/{id}/commands", id).contentType(MediaType.TEXT_PLAIN).content("MOVE\nREPORT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outputs", contains("3,1,EAST")));
    }

    @Test
    void statePersistsAcrossRequests() throws Exception {
        String id = newRobot();
        json(post("/robots/{id}/commands", id), "{\"command\":\"PLACE 0,0,NORTH\"}").andExpect(status().isOk());
        json(post("/robots/{id}/commands", id), "{\"command\":\"MOVE\"}").andExpect(status().isOk());
        mvc.perform(get("/robots/{id}/report", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.output").value("0,1,NORTH"));
    }

    @Test
    void moveThatWouldFallIsReportedAsIgnoredAndLaterMovesStillWork() throws Exception {
        String id = newRobot();
        json(post("/robots/{id}/commands", id), "{\"commands\":\"PLACE 0,4,NORTH MOVE RIGHT MOVE REPORT\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[1].command").value("MOVE"))
                .andExpect(jsonPath("$.results[1].status").value("IGNORED"))
                .andExpect(jsonPath("$.results[1].reason").value("WOULD_FALL"))
                .andExpect(jsonPath("$.results[0].ignored").doesNotExist())
                .andExpect(jsonPath("$.outputs", contains("1,4,EAST")));
    }

    @Test
    void putPlacementPlacesRobotAndRejectsOffTablePositionsWithoutChangingState() throws Exception {
        String id = newRobot();
        json(put("/robots/{id}/placement", id), "{\"x\":2,\"y\":3,\"facing\":\"south\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report").value("2,3,SOUTH"));

        json(put("/robots/{id}/placement", id), "{\"x\":5,\"y\":0,\"facing\":\"NORTH\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("OFF_TABLE"));

        mvc.perform(get("/robots/{id}", id)).andExpect(jsonPath("$.report").value("2,3,SOUTH"));
    }

    @Test
    void invalidInputReturns4xxAndExecutesNothing() throws Exception {
        String id = newRobot();
        json(post("/robots/{id}/commands", id), "{\"commands\":\"PLACE 0,0,NORTH JUMP\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_COMMAND"));
        mvc.perform(get("/robots/{id}", id)).andExpect(jsonPath("$.placed").value(false));

        json(post("/robots/{id}/commands", id), "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_JSON"));
        json(post("/robots/{id}/commands", id), "{\"commands\":42}")
                .andExpect(status().isBadRequest());
        mvc.perform(post("/robots/{id}/commands", id).contentType(MediaType.APPLICATION_XML).content("<MOVE/>"))
                .andExpect(status().isUnsupportedMediaType());
        json(put("/robots/{id}/placement", id), "{\"x\":\"1\",\"y\":1,\"facing\":\"NORTH\"}")
                .andExpect(status().isBadRequest());
        json(put("/robots/{id}/placement", id), "{\"x\":1.5,\"y\":1,\"facing\":\"NORTH\"}")
                .andExpect(status().isBadRequest());
        json(put("/robots/{id}/placement", id), "{\"x\":1,\"y\":1,\"facing\":\"UP\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PLACEMENT"));
    }

    @Test
    void unknownRobotsRoutesAndMethods() throws Exception {
        mvc.perform(get("/robots/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROBOT_NOT_FOUND"));
        json(post("/robots/nope/commands"), "{\"commands\":\"MOVE\"}").andExpect(status().isNotFound());
        mvc.perform(get("/nowhere")).andExpect(status().isNotFound());
        mvc.perform(patch("/robots"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("POST")));
    }

    @Test
    void robotsAreIndependentAndCanBeDeleted() throws Exception {
        String a = newRobot();
        String b = newRobot();
        json(post("/robots/{id}/commands", a), "{\"commands\":\"PLACE 1,1,NORTH\"}").andExpect(status().isOk());
        mvc.perform(get("/robots/{id}/report", b)).andExpect(jsonPath("$.output").value("ROBOT MISSING"));

        mvc.perform(delete("/robots/{id}", a)).andExpect(status().isNoContent());
        mvc.perform(get("/robots/{id}", a)).andExpect(status().isNotFound());
        mvc.perform(delete("/robots/{id}", a)).andExpect(status().isNotFound());
    }
}
