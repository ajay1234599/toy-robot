package com.example.toyrobot.command;

import com.example.toyrobot.command.CommandResult.Reason;
import com.example.toyrobot.constant.AppConstants.LogMessages;
import com.example.toyrobot.constant.AppConstants.Formats;
import com.example.toyrobot.domain.Position;
import com.example.toyrobot.domain.Table;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies commands to a robot state. The state is {@code null} while the robot is not on the table.
 * A command that would put the robot off the table leaves the state unchanged.
 */
public final class Simulator {

    private static final Logger log = LoggerFactory.getLogger(Simulator.class);

    public static final String ROBOT_MISSING = Formats.ROBOT_MISSING;

    public record Step(Position state, CommandResult result) {
    }

    public record Run(Position state, List<CommandResult> results) {

        public List<String> outputs() {
            return results.stream().map(CommandResult::output).filter(o -> o != null).toList();
        }
    }

    private Simulator() {
    }

    public static Step execute(Table table, Position state, Command command) {
        return switch (command) {
            case Command.Place place -> {
                // Bounds are checked on the raw coordinates, so the narrowing to int is always safe.
                yield table.contains(place.x(), place.y())
                        ? new Step(new Position((int) place.x(), (int) place.y(), place.facing()),
                                CommandResult.applied(place))
                        : new Step(state, CommandResult.ignored(place, Reason.OFF_TABLE));
            }
            case Command.Report report when state == null ->
                    new Step(null, CommandResult.ignored(report, Reason.NOT_PLACED, ROBOT_MISSING));
            case Command other when state == null ->
                    new Step(null, CommandResult.ignored(other, Reason.NOT_PLACED));
            case Command.Move move -> {
                Position next = state.forward();
                yield table.contains(next)
                        ? new Step(next, CommandResult.applied(move))
                        : new Step(state, CommandResult.ignored(move, Reason.WOULD_FALL));
            }
            case Command.Left left -> new Step(state.turnedLeft(), CommandResult.applied(left));
            case Command.Right right -> new Step(state.turnedRight(), CommandResult.applied(right));
            case Command.Report report -> new Step(state, CommandResult.applied(report, state.report()));
        };
    }

    /** Human-readable state for log lines: {@code X,Y,F} or "not placed". */
    public static String describe(Position state) {
        return state == null ? LogMessages.NOT_PLACED : state.report();
    }

    public static Run run(Table table, Position initialState, List<Command> commands) {
        Position state = initialState;
        List<CommandResult> results = new ArrayList<>(commands.size());
        for (Command command : commands) {
            Step step = execute(table, state, command);
            if (log.isDebugEnabled()) {
                CommandResult result = step.result();
                if (result.isIgnored()) {
                    log.debug(LogMessages.COMMAND_IGNORED, result.command(), result.reason(), describe(state));
                } else {
                    log.debug(LogMessages.COMMAND_APPLIED, result.command(), describe(state), describe(step.state()));
                }
            }
            state = step.state();
            results.add(step.result());
        }
        return new Run(state, List.copyOf(results));
    }
}
