package com.example.toyrobot.constant;

/** Text and example payloads for the OpenAPI documentation. Values are compile-time constants for annotations. */
public final class ApiDocs {

    private ApiDocs() {
    }

    public static final class Info {
        public static final String TITLE = "Toy Robot API";
        public static final String VERSION = "1.0.0";
        public static final String SERVER_URL = "http://localhost:8000";
        public static final String SERVER_DESCRIPTION = "Local development server";
        public static final String SPEC_FILE = "openapi.yaml";
        public static final String DESCRIPTION = """
                A truck robot on a 5 x 5 tabletop. (0,0) is the SOUTH WEST corner.
                Any placement or move that would put the robot off the table is ignored; \
                later valid commands still work. A robot that is not on the table ignores \
                MOVE, LEFT and RIGHT, and REPORT answers `ROBOT MISSING`.""";

        private Info() {
        }
    }

    public static final class Tags {
        public static final String ROBOTS = "Robots";
        public static final String ROBOTS_DESCRIPTION = "Create, inspect and delete robots";
        public static final String CONTROL = "Robot control";
        public static final String CONTROL_DESCRIPTION = "PLACE, MOVE, LEFT, RIGHT and REPORT";

        private Tags() {
        }
    }

    /** HTTP status codes as strings, as required by {@code @ApiResponse(responseCode)}. */
    public static final class Status {
        public static final String OK = "200";
        public static final String CREATED = "201";
        public static final String NO_CONTENT = "204";
        public static final String BAD_REQUEST = "400";
        public static final String NOT_FOUND = "404";
        public static final String PAYLOAD_TOO_LARGE = "413";
        public static final String UNSUPPORTED_MEDIA_TYPE = "415";
        public static final String UNPROCESSABLE_ENTITY = "422";

        private Status() {
        }
    }

    public static final class Schemas {
        public static final String STRING = "string";

        private Schemas() {
        }
    }

    public static final class Params {
        public static final String ID = "id";
        public static final String ID_DESCRIPTION = "Robot id returned by POST /robots";
        public static final String ID_EXAMPLE = "3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e";

        private Params() {
        }
    }

    public static final class Operations {
        public static final String CREATE_ID = "createRobot";
        public static final String LIST_ID = "listRobots";
        public static final String GET_ID = "getRobot";
        public static final String DELETE_ID = "deleteRobot";
        public static final String PLACE_ID = "placeRobot";
        public static final String REPORT_ID = "reportRobot";
        public static final String COMMANDS_ID = "runCommands";
        public static final String CREATE = "Create a robot";
        public static final String CREATE_DESCRIPTION =
                "Creates a robot that is not yet on the table. The Location header points at the new robot.";
        public static final String LIST = "List robots";
        public static final String LIST_DESCRIPTION = "Returns every robot and its current state.";
        public static final String GET = "Get a robot";
        public static final String GET_DESCRIPTION = "Returns the robot's current position and links.";
        public static final String DELETE = "Delete a robot";
        public static final String DELETE_DESCRIPTION = "Removes the robot. Later requests for it return 404.";
        public static final String PLACE = "PLACE the robot";
        public static final String PLACE_DESCRIPTION = """
                Puts the robot at X,Y facing F. Idempotent. A position off the table is rejected \
                with 422 and the robot stays where it was. `facing` is case-insensitive.""";
        public static final String REPORT = "REPORT the robot's position";
        public static final String REPORT_DESCRIPTION =
                "Returns X,Y,F, or `ROBOT MISSING` if the robot has not been placed.";
        public static final String COMMANDS = "Run commands";
        public static final String COMMANDS_DESCRIPTION = """
                Runs PLACE, MOVE, LEFT, RIGHT and REPORT in order. Commands that would make the robot \
                fall, or that arrive before a valid PLACE, are ignored and later commands still run; \
                each result says whether it was APPLIED or IGNORED and why. The whole batch is parsed \
                first, so a syntax error returns 400 and runs nothing. At most 10,000 commands and \
                1 MB per request. Send JSON, or `text/plain` with one command per line or space-separated.""";

        private Operations() {
        }
    }

    public static final class Responses {
        public static final String CREATED = "Robot created";
        public static final String ROBOT = "The robot";
        public static final String ROBOTS = "All robots";
        public static final String DELETED = "Robot deleted";
        public static final String PLACED = "Robot placed";
        public static final String REPORT = "Current position";
        public static final String COMMANDS = "Commands ran; see each result's status";
        public static final String NOT_FOUND = "No robot with this id";
        public static final String INVALID_PLACEMENT = "Body is not {x, y, facing} with integer x and y and a valid direction";
        public static final String OFF_TABLE = "Position is off the table; the robot is unchanged";
        public static final String INVALID_COMMANDS = "Unknown command, bad direction, malformed PLACE or malformed JSON; nothing ran";
        public static final String TOO_LARGE = "More than 10,000 commands, or a body over 1 MB; nothing ran";
        public static final String UNSUPPORTED_MEDIA_TYPE = "Content-Type is not application/json (or text/plain for commands)";

        private Responses() {
        }
    }

    public static final class RequestBodies {
        public static final String PLACEMENT = "Where to put the robot";
        public static final String COMMANDS = "Commands as a string, a list of strings, or a single command";

        private RequestBodies() {
        }
    }

    /** Names shown in the example drop-downs. */
    public static final class ExampleNames {
        public static final String PLACED = "Placed robot";
        public static final String UNPLACED = "Robot not on the table";
        public static final String SEQUENCE = "Command sequence";
        public static final String LIST = "List of commands";
        public static final String SINGLE = "Single command";
        public static final String PLAIN_TEXT = "Plain text";
        public static final String IGNORED_MOVE = "Move that would fall is ignored";
        public static final String NOT_PLACED = "Commands before PLACE";

        private ExampleNames() {
        }
    }

    /** Example payloads. They match what the API actually sends. */
    public static final class Examples {

        public static final String PLACEMENT_REQUEST = """
                { "x": 1, "y": 2, "facing": "EAST" }""";

        public static final String COMMANDS_STRING_REQUEST = """
                { "commands": "PLACE 1,2,EAST MOVE MOVE LEFT MOVE REPORT" }""";

        public static final String COMMANDS_LIST_REQUEST = """
                { "commands": ["PLACE 1,2,EAST", "MOVE", "MOVE", "LEFT", "MOVE", "REPORT"] }""";

        public static final String COMMAND_SINGLE_REQUEST = """
                { "command": "MOVE" }""";

        public static final String COMMANDS_TEXT_REQUEST = """
                PLACE 1,2,EAST
                MOVE
                MOVE
                LEFT
                MOVE
                REPORT""";

        public static final String ROBOT_PLACED = """
                {
                  "id": "3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e",
                  "placed": true,
                  "position": { "x": 1, "y": 2, "facing": "EAST" },
                  "report": "1,2,EAST",
                  "table": { "width": 5, "height": 5 },
                  "links": {
                    "self": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e",
                    "placement": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/placement",
                    "commands": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/commands",
                    "report": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/report"
                  }
                }""";

        public static final String ROBOT_UNPLACED = """
                {
                  "id": "3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e",
                  "placed": false,
                  "position": null,
                  "report": "ROBOT MISSING",
                  "table": { "width": 5, "height": 5 },
                  "links": {
                    "self": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e",
                    "placement": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/placement",
                    "commands": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/commands",
                    "report": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/report"
                  }
                }""";

        public static final String ROBOT_LIST = """
                {
                  "robots": [
                    {
                      "id": "3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e",
                      "placed": true,
                      "position": { "x": 1, "y": 2, "facing": "EAST" },
                      "report": "1,2,EAST",
                      "table": { "width": 5, "height": 5 },
                      "links": {
                        "self": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e",
                        "placement": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/placement",
                        "commands": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/commands",
                        "report": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/report"
                      }
                    }
                  ]
                }""";

        public static final String REPORT_PLACED = """
                { "output": "3,3,NORTH", "placed": true, "position": { "x": 3, "y": 3, "facing": "NORTH" } }""";

        public static final String REPORT_UNPLACED = """
                { "output": "ROBOT MISSING", "placed": false, "position": null }""";

        public static final String COMMANDS_RESPONSE = """
                {
                  "outputs": ["3,3,NORTH"],
                  "results": [
                    { "command": "PLACE 1,2,EAST", "status": "APPLIED" },
                    { "command": "MOVE", "status": "APPLIED" },
                    { "command": "MOVE", "status": "APPLIED" },
                    { "command": "LEFT", "status": "APPLIED" },
                    { "command": "MOVE", "status": "APPLIED" },
                    { "command": "REPORT", "status": "APPLIED", "output": "3,3,NORTH" }
                  ],
                  "robot": {
                    "id": "3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e",
                    "placed": true,
                    "position": { "x": 3, "y": 3, "facing": "NORTH" },
                    "report": "3,3,NORTH",
                    "table": { "width": 5, "height": 5 },
                    "links": {
                      "self": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e",
                      "placement": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/placement",
                      "commands": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/commands",
                      "report": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/report"
                    }
                  }
                }""";

        public static final String COMMANDS_IGNORED_RESPONSE = """
                {
                  "outputs": ["1,4,EAST"],
                  "results": [
                    { "command": "PLACE 0,4,NORTH", "status": "APPLIED" },
                    { "command": "MOVE", "status": "IGNORED", "reason": "WOULD_FALL" },
                    { "command": "RIGHT", "status": "APPLIED" },
                    { "command": "MOVE", "status": "APPLIED" },
                    { "command": "REPORT", "status": "APPLIED", "output": "1,4,EAST" }
                  ],
                  "robot": {
                    "id": "3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e",
                    "placed": true,
                    "position": { "x": 1, "y": 4, "facing": "EAST" },
                    "report": "1,4,EAST",
                    "table": { "width": 5, "height": 5 },
                    "links": {
                      "self": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e",
                      "placement": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/placement",
                      "commands": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/commands",
                      "report": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/report"
                    }
                  }
                }""";

        public static final String COMMANDS_NOT_PLACED_RESPONSE = """
                {
                  "outputs": ["ROBOT MISSING"],
                  "results": [
                    { "command": "MOVE", "status": "IGNORED", "reason": "NOT_PLACED" },
                    { "command": "REPORT", "status": "IGNORED", "reason": "NOT_PLACED", "output": "ROBOT MISSING" }
                  ],
                  "robot": {
                    "id": "3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e",
                    "placed": false,
                    "position": null,
                    "report": "ROBOT MISSING",
                    "table": { "width": 5, "height": 5 },
                    "links": {
                      "self": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e",
                      "placement": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/placement",
                      "commands": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/commands",
                      "report": "/robots/3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e/report"
                    }
                  }
                }""";

        public static final String ERROR_NOT_FOUND = """
                { "error": { "code": "ROBOT_NOT_FOUND", "message": "No robot with id 3f2b6c1e-8a4d-4f7b-9c2e-1d5a6b7c8d9e" } }""";

        public static final String ERROR_OFF_TABLE = """
                { "error": { "code": "OFF_TABLE", "message": "Position (5,0) is outside the 5x5 table; placement ignored" } }""";

        public static final String ERROR_INVALID_PLACEMENT = """
                { "error": { "code": "INVALID_PLACEMENT", "message": "\\"facing\\" must be one of NORTH, EAST, SOUTH, WEST" } }""";

        public static final String ERROR_INVALID_COMMAND = """
                { "error": { "code": "INVALID_COMMAND", "message": "Unknown command \\"JUMP\\"" } }""";

        public static final String ERROR_TOO_MANY_COMMANDS = """
                { "error": { "code": "TOO_MANY_COMMANDS", "message": "A request may contain at most 10000 commands" } }""";

        public static final String ERROR_UNSUPPORTED_MEDIA_TYPE = """
                { "error": { "code": "UNSUPPORTED_MEDIA_TYPE", "message": "Supported content types: [application/json]" } }""";

        private Examples() {
        }
    }
}
