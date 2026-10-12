package com.example.toyrobot.api;

import com.example.toyrobot.command.Command;
import com.example.toyrobot.command.CommandParser;
import com.example.toyrobot.command.CommandResult;
import com.example.toyrobot.command.Simulator;
import com.example.toyrobot.constant.AppConstants.ApiPaths;
import com.example.toyrobot.constant.AppConstants.ErrorCodes;
import com.example.toyrobot.constant.AppConstants.ErrorMessages;
import com.example.toyrobot.constant.AppConstants.JsonFields;
import com.example.toyrobot.constant.AppConstants.LogMessages;
import com.example.toyrobot.domain.Direction;
import com.example.toyrobot.domain.Position;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.toyrobot.constant.ApiDocs;
import com.example.toyrobot.constant.ApiDocs.ExampleNames;
import com.example.toyrobot.constant.ApiDocs.Examples;
import com.example.toyrobot.constant.ApiDocs.Operations;
import com.example.toyrobot.constant.ApiDocs.Params;
import com.example.toyrobot.constant.ApiDocs.Responses;
import com.example.toyrobot.constant.ApiDocs.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.ROBOTS)
public class RobotController {

    private static final Logger log = LoggerFactory.getLogger(RobotController.class);

    private final RobotRepository robots;

    public RobotController(RobotRepository robots) {
        this.robots = robots;
    }

    @Operation(operationId = Operations.CREATE_ID, summary = Operations.CREATE, description = Operations.CREATE_DESCRIPTION, tags = ApiDocs.Tags.ROBOTS,
            responses = {
            @ApiResponse(responseCode = Status.CREATED,
                    description = Responses.CREATED,
                    headers = @Header(name = HttpHeaders.LOCATION, description = Params.ID_DESCRIPTION, schema = @Schema(type = ApiDocs.Schemas.STRING)),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RobotResponse.class),
                    examples = {@ExampleObject(value = Examples.ROBOT_UNPLACED)}))
            })
    @PostMapping
    ResponseEntity<RobotResponse> create() {
        Robot robot = robots.create();
        log.info(LogMessages.ROBOT_CREATED, robot.id());
        return ResponseEntity.created(URI.create(ApiPaths.robot(robot.id()))).body(RobotResponse.of(robot));
    }

    @Operation(operationId = Operations.LIST_ID, summary = Operations.LIST, description = Operations.LIST_DESCRIPTION, tags = ApiDocs.Tags.ROBOTS,
            responses = {
            @ApiResponse(responseCode = Status.OK,
                    description = Responses.ROBOTS,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RobotListResponse.class),
                    examples = {@ExampleObject(value = Examples.ROBOT_LIST)}))
            })
    @GetMapping
    RobotListResponse list() {
        List<RobotResponse> all = robots.findAll().stream().map(RobotResponse::of).toList();
        log.debug(LogMessages.ROBOTS_LISTED, all.size());
        return new RobotListResponse(all);
    }

    @Operation(operationId = Operations.GET_ID, summary = Operations.GET, description = Operations.GET_DESCRIPTION, tags = ApiDocs.Tags.ROBOTS,
            responses = {
            @ApiResponse(responseCode = Status.OK,
                    description = Responses.ROBOT,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RobotResponse.class),
                    examples = {@ExampleObject(name = ExampleNames.PLACED, value = Examples.ROBOT_PLACED), @ExampleObject(name = ExampleNames.UNPLACED, value = Examples.ROBOT_UNPLACED)})),
            @ApiResponse(responseCode = Status.NOT_FOUND,
                    description = Responses.NOT_FOUND,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_NOT_FOUND)}))
            })
    @GetMapping(ApiPaths.ROBOT_ID)
    RobotResponse get(
            @Parameter(name = Params.ID, in = ParameterIn.PATH, description = Params.ID_DESCRIPTION, example = Params.ID_EXAMPLE) @PathVariable String id) {
        RobotResponse robot = RobotResponse.of(find(id));
        log.debug(LogMessages.ROBOT_FETCHED, id);
        return robot;
    }

    @Operation(operationId = Operations.DELETE_ID, summary = Operations.DELETE, description = Operations.DELETE_DESCRIPTION, tags = ApiDocs.Tags.ROBOTS,
            responses = {
            @ApiResponse(responseCode = Status.NO_CONTENT, description = Responses.DELETED),
            @ApiResponse(responseCode = Status.NOT_FOUND,
                    description = Responses.NOT_FOUND,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_NOT_FOUND)}))
            })
    @DeleteMapping(ApiPaths.ROBOT_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @Parameter(name = Params.ID, in = ParameterIn.PATH, description = Params.ID_DESCRIPTION, example = Params.ID_EXAMPLE) @PathVariable String id) {
        if (!robots.delete(id)) {
            throw ApiException.robotNotFound(id);
        }
        log.info(LogMessages.ROBOT_DELETED, id);
    }


    @Operation(operationId = Operations.PLACE_ID, summary = Operations.PLACE, description = Operations.PLACE_DESCRIPTION, tags = ApiDocs.Tags.CONTROL,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = ApiDocs.RequestBodies.PLACEMENT,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlacementRequest.class),
                            examples = @ExampleObject(value = Examples.PLACEMENT_REQUEST))),
            responses = {
            @ApiResponse(responseCode = Status.OK,
                    description = Responses.PLACED,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RobotResponse.class),
                    examples = {@ExampleObject(value = Examples.ROBOT_PLACED)})),
            @ApiResponse(responseCode = Status.BAD_REQUEST,
                    description = Responses.INVALID_PLACEMENT,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_INVALID_PLACEMENT)})),
            @ApiResponse(responseCode = Status.NOT_FOUND,
                    description = Responses.NOT_FOUND,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_NOT_FOUND)})),
            @ApiResponse(responseCode = Status.UNSUPPORTED_MEDIA_TYPE,
                    description = Responses.UNSUPPORTED_MEDIA_TYPE,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_UNSUPPORTED_MEDIA_TYPE)})),
            @ApiResponse(responseCode = Status.UNPROCESSABLE_ENTITY,
                    description = Responses.OFF_TABLE,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_OFF_TABLE)}))
            })
    @PutMapping(path = ApiPaths.ROBOT_PLACEMENT, consumes = MediaType.APPLICATION_JSON_VALUE)
    RobotResponse place(
            @Parameter(name = Params.ID, in = ParameterIn.PATH, description = Params.ID_DESCRIPTION, example = Params.ID_EXAMPLE) @PathVariable String id, @RequestBody JsonNode body) {
        Robot robot = find(id);
        Command.Place place = placementFrom(body);
        log.debug(LogMessages.PLACE_REQUESTED, id, place.x(), place.y(), place.facing());
        Simulator.Run run = robot.execute(List.of(place));
        if (run.results().getFirst().isIgnored()) {
            log.info(LogMessages.PLACE_REJECTED, id, place.x(), place.y(), place.facing());
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.OFF_TABLE,
                    ErrorMessages.OFF_TABLE.formatted(
                            place.x(), place.y(), robot.table().width(), robot.table().height()));
        }
        log.info(LogMessages.PLACE_APPLIED, id, run.state().report());
        return RobotResponse.of(robot, run.state());
    }

    @Operation(operationId = Operations.REPORT_ID, summary = Operations.REPORT, description = Operations.REPORT_DESCRIPTION, tags = ApiDocs.Tags.CONTROL,
            responses = {
            @ApiResponse(responseCode = Status.OK,
                    description = Responses.REPORT,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ReportResponse.class),
                    examples = {@ExampleObject(name = ExampleNames.PLACED, value = Examples.REPORT_PLACED), @ExampleObject(name = ExampleNames.UNPLACED, value = Examples.REPORT_UNPLACED)})),
            @ApiResponse(responseCode = Status.NOT_FOUND,
                    description = Responses.NOT_FOUND,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_NOT_FOUND)}))
            })
    @GetMapping(ApiPaths.ROBOT_REPORT)
    ReportResponse report(
            @Parameter(name = Params.ID, in = ParameterIn.PATH, description = Params.ID_DESCRIPTION, example = Params.ID_EXAMPLE) @PathVariable String id) {
        Position position = find(id).position();
        String output = position != null ? position.report() : Simulator.ROBOT_MISSING;
        log.debug(LogMessages.REPORT_REQUESTED, id, output);
        return new ReportResponse(output, position != null, position);
    }

    @Operation(operationId = Operations.COMMANDS_ID, summary = Operations.COMMANDS, description = Operations.COMMANDS_DESCRIPTION, tags = ApiDocs.Tags.CONTROL,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = ApiDocs.RequestBodies.COMMANDS,
                    content = {
                        @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommandsRequest.class),
                                examples = {
                                    @ExampleObject(name = ExampleNames.SEQUENCE, value = Examples.COMMANDS_STRING_REQUEST),
                                    @ExampleObject(name = ExampleNames.LIST, value = Examples.COMMANDS_LIST_REQUEST),
                                    @ExampleObject(name = ExampleNames.SINGLE, value = Examples.COMMAND_SINGLE_REQUEST)}),
                        @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = ApiDocs.Schemas.STRING),
                                examples = @ExampleObject(name = ExampleNames.PLAIN_TEXT, value = Examples.COMMANDS_TEXT_REQUEST))
                    }),
            responses = {
            @ApiResponse(responseCode = Status.OK,
                    description = Responses.COMMANDS,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommandsResponse.class),
                    examples = {@ExampleObject(name = ExampleNames.SEQUENCE, value = Examples.COMMANDS_RESPONSE), @ExampleObject(name = ExampleNames.IGNORED_MOVE, value = Examples.COMMANDS_IGNORED_RESPONSE), @ExampleObject(name = ExampleNames.NOT_PLACED, value = Examples.COMMANDS_NOT_PLACED_RESPONSE)})),
            @ApiResponse(responseCode = Status.BAD_REQUEST,
                    description = Responses.INVALID_COMMANDS,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_INVALID_COMMAND)})),
            @ApiResponse(responseCode = Status.NOT_FOUND,
                    description = Responses.NOT_FOUND,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_NOT_FOUND)})),
            @ApiResponse(responseCode = Status.PAYLOAD_TOO_LARGE,
                    description = Responses.TOO_LARGE,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_TOO_MANY_COMMANDS)})),
            @ApiResponse(responseCode = Status.UNSUPPORTED_MEDIA_TYPE,
                    description = Responses.UNSUPPORTED_MEDIA_TYPE,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_UNSUPPORTED_MEDIA_TYPE)}))
            })
    @PostMapping(path = ApiPaths.ROBOT_COMMANDS, consumes = MediaType.APPLICATION_JSON_VALUE)
    CommandsResponse runJson(
            @Parameter(name = Params.ID, in = ParameterIn.PATH, description = Params.ID_DESCRIPTION, example = Params.ID_EXAMPLE) @PathVariable String id, @RequestBody JsonNode body) {
        Robot robot = find(id);
        List<Command> commands = commandsFrom(body);
        log.debug(LogMessages.COMMANDS_RECEIVED, id, commands.size(), LogMessages.JSON);
        return run(robot, commands);
    }

    @Operation(operationId = Operations.COMMANDS_ID, summary = Operations.COMMANDS, description = Operations.COMMANDS_DESCRIPTION, tags = ApiDocs.Tags.CONTROL,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = ApiDocs.RequestBodies.COMMANDS,
                    content = {
                        @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommandsRequest.class),
                                examples = {
                                    @ExampleObject(name = ExampleNames.SEQUENCE, value = Examples.COMMANDS_STRING_REQUEST),
                                    @ExampleObject(name = ExampleNames.LIST, value = Examples.COMMANDS_LIST_REQUEST),
                                    @ExampleObject(name = ExampleNames.SINGLE, value = Examples.COMMAND_SINGLE_REQUEST)}),
                        @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = ApiDocs.Schemas.STRING),
                                examples = @ExampleObject(name = ExampleNames.PLAIN_TEXT, value = Examples.COMMANDS_TEXT_REQUEST))
                    }),
            responses = {
            @ApiResponse(responseCode = Status.OK,
                    description = Responses.COMMANDS,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommandsResponse.class),
                    examples = {@ExampleObject(name = ExampleNames.SEQUENCE, value = Examples.COMMANDS_RESPONSE), @ExampleObject(name = ExampleNames.IGNORED_MOVE, value = Examples.COMMANDS_IGNORED_RESPONSE), @ExampleObject(name = ExampleNames.NOT_PLACED, value = Examples.COMMANDS_NOT_PLACED_RESPONSE)})),
            @ApiResponse(responseCode = Status.BAD_REQUEST,
                    description = Responses.INVALID_COMMANDS,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_INVALID_COMMAND)})),
            @ApiResponse(responseCode = Status.NOT_FOUND,
                    description = Responses.NOT_FOUND,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_NOT_FOUND)})),
            @ApiResponse(responseCode = Status.PAYLOAD_TOO_LARGE,
                    description = Responses.TOO_LARGE,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_TOO_MANY_COMMANDS)})),
            @ApiResponse(responseCode = Status.UNSUPPORTED_MEDIA_TYPE,
                    description = Responses.UNSUPPORTED_MEDIA_TYPE,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class),
                    examples = {@ExampleObject(value = Examples.ERROR_UNSUPPORTED_MEDIA_TYPE)}))
            })
    @PostMapping(path = ApiPaths.ROBOT_COMMANDS, consumes = MediaType.TEXT_PLAIN_VALUE)
    CommandsResponse runText(
            @Parameter(name = Params.ID, in = ParameterIn.PATH, description = Params.ID_DESCRIPTION, example = Params.ID_EXAMPLE) @PathVariable String id, @RequestBody String body) {
        Robot robot = find(id);
        List<Command> commands = CommandParser.parse(body);
        log.debug(LogMessages.COMMANDS_RECEIVED, id, commands.size(), LogMessages.TEXT);
        return run(robot, commands);
    }

    private CommandsResponse run(Robot robot, List<Command> commands) {
        Simulator.Run run = robot.execute(commands);
        long ignored = run.results().stream().filter(CommandResult::isIgnored).count();
        log.info(LogMessages.COMMANDS_COMPLETED, robot.id(), commands.size(), commands.size() - ignored, ignored,
                Simulator.describe(run.state()));
        return new CommandsResponse(run.outputs(), run.results(), RobotResponse.of(robot, run.state()));
    }

    private Robot find(String id) {
        return robots.find(id).orElseThrow(() -> ApiException.robotNotFound(id));
    }

    private static Command.Place placementFrom(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw ApiException.badRequest(ErrorCodes.INVALID_PLACEMENT, ErrorMessages.PLACEMENT_NOT_OBJECT);
        }
        JsonNode x = body.get(JsonFields.X);
        JsonNode y = body.get(JsonFields.Y);
        if (x == null || y == null || !x.isIntegralNumber() || !y.isIntegralNumber()) {
            throw ApiException.badRequest(ErrorCodes.INVALID_PLACEMENT, ErrorMessages.COORDINATES_NOT_INTEGERS);
        }
        JsonNode facingNode = body.get(JsonFields.FACING);
        Direction facing = (facingNode != null && facingNode.isTextual()
                ? Direction.parse(facingNode.asText())
                : Optional.<Direction>empty())
                .orElseThrow(() -> ApiException.badRequest(ErrorCodes.INVALID_PLACEMENT,
                        ErrorMessages.INVALID_FACING.formatted(CommandParser.DIRECTIONS)));
        return new Command.Place(
                CommandParser.saturatedLong(x.bigIntegerValue()), CommandParser.saturatedLong(y.bigIntegerValue()), facing);
    }

    private static List<Command> commandsFrom(JsonNode body) {
        JsonNode node = body;
        if (body != null && body.isObject()) {
            node = body.has(JsonFields.COMMANDS) ? body.get(JsonFields.COMMANDS) : body.get(JsonFields.COMMAND);
        }
        if (node != null && node.isTextual()) {
            return CommandParser.parse(node.asText());
        }
        if (node != null && node.isArray()) {
            List<String> lines = new ArrayList<>();
            for (JsonNode item : node) {
                if (!item.isTextual()) {
                    throw ApiException.badRequest(ErrorCodes.INVALID_COMMAND, ErrorMessages.COMMANDS_NOT_STRINGS);
                }
                lines.add(item.asText());
            }
            return CommandParser.parse(lines);
        }
        throw ApiException.badRequest(ErrorCodes.INVALID_COMMAND, ErrorMessages.COMMANDS_USAGE);
    }
}
