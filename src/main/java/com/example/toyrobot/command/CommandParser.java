package com.example.toyrobot.command;

import com.example.toyrobot.constant.AppConstants.Commands;
import com.example.toyrobot.constant.AppConstants.ErrorMessages;
import com.example.toyrobot.constant.AppConstants.Limits;
import com.example.toyrobot.constant.AppConstants.LogMessages;
import com.example.toyrobot.domain.Direction;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommandParser {

    private static final Logger log = LoggerFactory.getLogger(CommandParser.class);
    private static final Pattern TOKEN = Pattern.compile(Commands.TOKEN_REGEX, Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACE_ARGS = Pattern.compile(Commands.PLACE_ARGS_REGEX, Pattern.CASE_INSENSITIVE);
    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    public static final int MAX_COMMANDS = Limits.MAX_COMMANDS;
    public static final String DIRECTIONS =
            Arrays.stream(Direction.values()).map(Enum::name).collect(Collectors.joining(Commands.DIRECTION_SEPARATOR));

    private CommandParser() {
    }

    public static List<Command> parse(String input) {
        List<Command> commands = new ArrayList<>();
        parseInto(input, commands);
        log.debug(LogMessages.COMMANDS_PARSED, commands.size(), input.length());
        return commands;
    }

    public static List<Command> parse(List<String> lines) {
        List<Command> commands = new ArrayList<>();
        int characters = 0;
        for (String line : lines) {
            parseInto(line, commands);
            characters += line.length();
        }
        log.debug(LogMessages.COMMANDS_PARSED, commands.size(), characters);
        return commands;
    }

    private static void parseInto(String input, List<Command> commands) {
        Matcher matcher = TOKEN.matcher(input);
        while (matcher.find()) {
            if (commands.size() == MAX_COMMANDS) {
                log.debug(LogMessages.TOO_MANY_COMMANDS, MAX_COMMANDS);
                throw new TooManyCommandsException(MAX_COMMANDS);
            }
            try {
                commands.add(parseCommand(matcher.group()));
            } catch (CommandParseException e) {
                log.debug(LogMessages.COMMAND_REJECTED, matcher.group(), e.getMessage());
                throw e;
            }
        }
    }

    static Command parseCommand(String token) {
        String upper = token.toUpperCase(Locale.ROOT);
        switch (upper) {
            case Commands.MOVE:
                return new Command.Move();
            case Commands.LEFT:
                return new Command.Left();
            case Commands.RIGHT:
                return new Command.Right();
            case Commands.REPORT:
                return new Command.Report();
            default:
                break;
        }

        if (!upper.startsWith(Commands.PLACE)) {
            throw new CommandParseException(ErrorMessages.UNKNOWN_COMMAND.formatted(token));
        }
        Matcher match = PLACE_ARGS.matcher(upper);
        if (!match.matches()) {
            throw new CommandParseException(ErrorMessages.MALFORMED_PLACE.formatted(token));
        }
        Direction facing = Direction.parse(match.group(3)).orElseThrow(() -> new CommandParseException(
                ErrorMessages.INVALID_DIRECTION.formatted(match.group(3), DIRECTIONS)));
        return new Command.Place(saturatedLong(match.group(1)), saturatedLong(match.group(2)), facing);
    }

    /** Coordinates beyond the long range are still syntactically valid; they just can't be on the table. */
    public static long saturatedLong(BigInteger value) {
        return value.max(LONG_MIN).min(LONG_MAX).longValue();
    }

    private static long saturatedLong(String digits) {
        return saturatedLong(new BigInteger(digits));
    }
}
