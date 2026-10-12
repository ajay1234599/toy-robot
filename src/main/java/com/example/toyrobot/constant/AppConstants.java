package com.example.toyrobot.constant;

/** Every string and limit used by the application, grouped by concern. */
public final class AppConstants {

    private AppConstants() {
    }

    /** URL paths. Values are compile-time constants so they can be used in mapping annotations. */
    public static final class ApiPaths {
        public static final String ROBOTS = "/robots";
        public static final String ROBOT_ID = "/{id}";
        public static final String PLACEMENT = "/placement";
        public static final String COMMANDS = "/commands";
        public static final String REPORT = "/report";
        public static final String ROBOT_PLACEMENT = ROBOT_ID + PLACEMENT;
        public static final String ROBOT_COMMANDS = ROBOT_ID + COMMANDS;
        public static final String ROBOT_REPORT = ROBOT_ID + REPORT;
        public static final String PATH_SEPARATOR = "/";

        private ApiPaths() {
        }

        public static String robot(String id) {
            return ROBOTS + PATH_SEPARATOR + id;
        }
    }

    /** Field names in request and response bodies. */
    public static final class JsonFields {
        public static final String X = "x";
        public static final String Y = "y";
        public static final String FACING = "facing";
        public static final String COMMAND = "command";
        public static final String COMMANDS = "commands";
        public static final String ROBOTS = "robots";
        public static final String LINK_SELF = "self";
        public static final String LINK_PLACEMENT = "placement";
        public static final String LINK_COMMANDS = "commands";
        public static final String LINK_REPORT = "report";

        private JsonFields() {
        }
    }

    /** Command keywords and the regular expressions used to tokenise them. */
    public static final class Commands {
        public static final String PLACE = "PLACE";
        public static final String MOVE = "MOVE";
        public static final String LEFT = "LEFT";
        public static final String RIGHT = "RIGHT";
        public static final String REPORT = "REPORT";
        /** PLACE plus its arguments is one token, tolerating spaces around the commas. */
        public static final String TOKEN_REGEX = PLACE + "\\s+[^\\s,]*\\s*,\\s*[^\\s,]*\\s*,\\s*[^\\s,]*|\\S+";
        public static final String PLACE_ARGS_REGEX = PLACE + "\\s+(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*([A-Z]+)";
        public static final String DIRECTION_SEPARATOR = ", ";

        private Commands() {
        }
    }

    /** Output formats. */
    public static final class Formats {
        /** {@code X,Y,F} as produced by REPORT. */
        public static final String POSITION = "%d,%d,%s";
        public static final String PLACE_LABEL = Commands.PLACE + " " + POSITION;
        public static final String ROBOT_MISSING = "ROBOT MISSING";

        private Formats() {
        }
    }

    /** Machine-readable error codes returned in {@code error.code}. */
    public static final class ErrorCodes {
        public static final String INVALID_COMMAND = "INVALID_COMMAND";
        public static final String INVALID_PLACEMENT = "INVALID_PLACEMENT";
        public static final String INVALID_JSON = "INVALID_JSON";
        public static final String OFF_TABLE = "OFF_TABLE";
        public static final String ROBOT_NOT_FOUND = "ROBOT_NOT_FOUND";
        public static final String NOT_FOUND = "NOT_FOUND";
        public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
        public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
        public static final String PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE";
        public static final String TOO_MANY_COMMANDS = "TOO_MANY_COMMANDS";
        public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

        private ErrorCodes() {
        }
    }

    /** Human-readable error messages. Those containing {@code %} are format strings. */
    public static final class ErrorMessages {
        public static final String ROBOT_NOT_FOUND = "No robot with id %s";
        public static final String NO_ROUTE = "No route for /%s";
        public static final String UNKNOWN_COMMAND = "Unknown command \"%s\"";
        public static final String MALFORMED_PLACE = "Malformed PLACE command \"%s\", expected \"PLACE X,Y,F\"";
        public static final String INVALID_DIRECTION = "Invalid direction \"%s\", expected one of %s";
        public static final String COMMANDS_NOT_STRINGS = "Commands must be strings";
        public static final String COMMANDS_USAGE =
                "Body must be {\"commands\": \"PLACE 0,0,NORTH MOVE REPORT\"}, {\"commands\": [...]}, "
                        + "{\"command\": \"MOVE\"} or text/plain";
        public static final String PLACEMENT_NOT_OBJECT = "Body must be a JSON object {\"x\", \"y\", \"facing\"}";
        public static final String COORDINATES_NOT_INTEGERS = "\"x\" and \"y\" must be integers";
        public static final String INVALID_FACING = "\"facing\" must be one of %s";
        public static final String OFF_TABLE = "Position (%d,%d) is outside the %dx%d table; placement ignored";
        public static final String INVALID_JSON = "Request body is missing or is not valid JSON";
        public static final String UNSUPPORTED_MEDIA_TYPE = "Supported content types: %s";
        public static final String BODY_TOO_LARGE = "Request body exceeds %d bytes";
        public static final String TOO_MANY_COMMANDS = "A request may contain at most %d commands";
        public static final String INTERNAL_ERROR = "Unexpected server error";
        public static final String INVALID_TABLE = "Table dimensions must be positive";
        public static final String FACING_REQUIRED = "facing";

        private ErrorMessages() {
        }
    }

    /** Request-tracing settings: every log line of a request carries its id. */
    public static final class Tracing {
        public static final String REQUEST_ID_HEADER = "X-Request-Id";
        public static final String MDC_REQUEST_ID = "requestId";
        /** Client-supplied ids are only reused if they're short and safe to write into a log line. */
        public static final String REQUEST_ID_PATTERN = "[A-Za-z0-9._-]{1,64}";

        private Tracing() {
        }
    }

    /** SLF4J messages for every layer; {@code {}} are placeholders. */
    public static final class LogMessages {
        // Web layer (filters)
        public static final String REQUEST_STARTED = "--> {} {}";
        public static final String REQUEST_COMPLETED = "<-- {} {} {} ({} ms)";
        public static final String REQUEST_FAILED = "<-- {} {} failed after {} ms";
        public static final String BODY_TOO_LARGE_DECLARED = "Rejected {} {}: declared body of {} bytes exceeds {} bytes";
        public static final String BODY_TOO_LARGE_STREAMED = "Rejected request body: streamed past {} bytes";

        // Controller layer
        public static final String ROBOT_CREATED = "Created robot {}";
        public static final String ROBOTS_LISTED = "Listed {} robots";
        public static final String ROBOT_FETCHED = "Fetched robot {}";
        public static final String ROBOT_DELETED = "Deleted robot {}";
        public static final String PLACE_REQUESTED = "Robot {}: PLACE {},{},{} requested";
        public static final String PLACE_APPLIED = "Robot {}: placed at {}";
        public static final String PLACE_REJECTED = "Robot {}: PLACE {},{},{} rejected, off the table";
        public static final String REPORT_REQUESTED = "Robot {}: REPORT -> {}";
        public static final String COMMANDS_RECEIVED = "Robot {}: received {} commands ({})";
        public static final String COMMANDS_COMPLETED = "Robot {}: {} commands, {} applied, {} ignored, now {}";

        // Domain / simulation layer
        public static final String BATCH_STARTED = "Robot {}: executing {} commands from {}";
        public static final String BATCH_FINISHED = "Robot {}: state {} -> {}";
        public static final String COMMAND_APPLIED = "{} applied: {} -> {}";
        public static final String COMMAND_IGNORED = "{} ignored ({}), robot stays at {}";
        public static final String COMMANDS_PARSED = "Parsed {} commands from {} characters";
        public static final String COMMAND_REJECTED = "Rejected command token \"{}\": {}";
        public static final String TOO_MANY_COMMANDS = "Rejected batch: more than {} commands";

        // Repository layer
        public static final String REPOSITORY_SAVED = "Stored robot {} ({} robots in store)";
        public static final String REPOSITORY_HIT = "Found robot {}";
        public static final String REPOSITORY_MISS = "No robot {} in store";
        public static final String REPOSITORY_LIST = "Listing {} robots";
        public static final String REPOSITORY_REMOVED = "Removed robot {} ({} robots in store)";
        public static final String REPOSITORY_REMOVE_MISS = "Delete of unknown robot {}";

        // Error handling
        public static final String CLIENT_ERROR = "{} {}: {}";
        public static final String UNEXPECTED_ERROR = "Unexpected error";

        // Labels used inside log lines
        public static final String NOT_PLACED = "not placed";
        public static final String JSON = "json";
        public static final String TEXT = "text";

        private LogMessages() {
        }
    }

    public static final class Limits {
        public static final int TABLE_WIDTH = 5;
        public static final int TABLE_HEIGHT = 5;
        /** Upper bound on commands per request, so a single request can't monopolise a robot. */
        public static final int MAX_COMMANDS = 10_000;
        public static final int MAX_BODY_BYTES = 1024 * 1024;

        private Limits() {
        }
    }
}
