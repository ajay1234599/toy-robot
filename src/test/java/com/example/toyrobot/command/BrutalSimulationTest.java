package com.example.toyrobot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.toyrobot.command.CommandResult.Reason;
import com.example.toyrobot.domain.Direction;
import com.example.toyrobot.domain.Position;
import com.example.toyrobot.domain.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Exhaustive and randomised checks of the simulation rules, independent of HTTP. */
class BrutalSimulationTest {

    private static final Table TABLE = Table.STANDARD;
    private static final String[] DIRS = {"NORTH", "EAST", "SOUTH", "WEST"};

    private static Simulator.Run simulate(String input) {
        return Simulator.run(TABLE, null, CommandParser.parse(input));
    }

    static Stream<Arguments> everyCellAndDirection() {
        return IntStream.range(0, 5).boxed().flatMap(x -> IntStream.range(0, 5).boxed()
                .flatMap(y -> Stream.of(DIRS).map(f -> Arguments.of(x, y, f))));
    }

    static Stream<Arguments> offTableRing() {
        List<Arguments> cases = new ArrayList<>();
        for (int x = -3; x <= 7; x++) {
            for (int y = -3; y <= 7; y++) {
                if (!TABLE.contains(x, y)) {
                    for (String f : DIRS) {
                        cases.add(Arguments.of(x, y, f));
                    }
                }
            }
        }
        return cases.stream();
    }

    @Nested
    @DisplayName("Exhaustive: every cell x every direction")
    class Exhaustive {

        @ParameterizedTest(name = "PLACE {0},{1},{2} REPORT")
        @MethodSource("com.example.toyrobot.command.BrutalSimulationTest#everyCellAndDirection")
        void placeIsValidOnEveryCell(int x, int y, String f) {
            assertThat(simulate("PLACE %d,%d,%s REPORT".formatted(x, y, f)).outputs())
                    .containsExactly("%d,%d,%s".formatted(x, y, f));
        }

        @ParameterizedTest(name = "PLACE {0},{1},{2} MOVE")
        @MethodSource("com.example.toyrobot.command.BrutalSimulationTest#everyCellAndDirection")
        void moveFromEveryCellNeverFalls(int x, int y, String f) {
            Direction d = Direction.valueOf(f);
            int nx = x + d.dx();
            int ny = y + d.dy();
            boolean inside = nx >= 0 && nx < 5 && ny >= 0 && ny < 5;
            Simulator.Run run = simulate("PLACE %d,%d,%s MOVE".formatted(x, y, f));
            assertThat(run.state()).isEqualTo(inside ? new Position(nx, ny, d) : new Position(x, y, d));
            assertThat(run.results().get(1).reason()).isEqualTo(inside ? null : Reason.WOULD_FALL);
        }

        @ParameterizedTest(name = "PLACE {0},{1},{2} then 4x LEFT / 4x RIGHT")
        @MethodSource("com.example.toyrobot.command.BrutalSimulationTest#everyCellAndDirection")
        void rotationNeverChangesPosition(int x, int y, String f) {
            String start = "%d,%d,%s".formatted(x, y, f);
            assertThat(simulate("PLACE " + start + " LEFT LEFT LEFT LEFT REPORT").outputs()).containsExactly(start);
            assertThat(simulate("PLACE " + start + " RIGHT RIGHT RIGHT RIGHT REPORT").outputs()).containsExactly(start);
            assertThat(simulate("PLACE " + start + " LEFT RIGHT RIGHT LEFT REPORT").outputs()).containsExactly(start);
            assertThat(simulate("PLACE " + start + " LEFT LEFT REPORT").state())
                    .isEqualTo(new Position(x, y, Direction.valueOf(f).right().right()));
        }

        @ParameterizedTest(name = "PLACE {0},{1},{2} is ignored")
        @MethodSource("com.example.toyrobot.command.BrutalSimulationTest#offTableRing")
        void offTablePlacementIsIgnoredWhetherOrNotPlaced(int x, int y, String f) {
            String bad = "PLACE %d,%d,%s".formatted(x, y, f);
            Simulator.Run fresh = simulate(bad + " MOVE LEFT RIGHT REPORT");
            assertThat(fresh.state()).isNull();
            assertThat(fresh.outputs()).containsExactly("ROBOT MISSING");
            assertThat(fresh.results().getFirst().reason()).isEqualTo(Reason.OFF_TABLE);

            Simulator.Run placed = simulate("PLACE 2,2,NORTH " + bad + " REPORT");
            assertThat(placed.outputs()).containsExactly("2,2,NORTH");
        }
    }

    @Nested
    @DisplayName("Extreme values")
    class Extremes {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
                "PLACE 2147483647,0,NORTH", "PLACE 2147483648,0,NORTH", "PLACE -2147483648,0,NORTH",
                "PLACE -2147483649,0,NORTH", "PLACE 0,9223372036854775807,NORTH", "PLACE 0,-9223372036854775808,NORTH",
                "PLACE 99999999999999999999999999999999,0,NORTH", "PLACE -99999999999999999999999999999999,0,NORTH",
                "PLACE 4294967296,0,NORTH", "PLACE 4294967298,2,NORTH", "PLACE 0,4294967300,EAST"})
        void hugeCoordinatesAreOffTableNotErrors(String place) {
            // 4294967298 would wrap to 2 if the code narrowed to int before checking bounds.
            Simulator.Run run = simulate(place + " REPORT");
            assertThat(run.results().getFirst().reason()).isEqualTo(Reason.OFF_TABLE);
            assertThat(run.outputs()).containsExactly("ROBOT MISSING");
        }

        @Test
        void negativeZeroAndLeadingZerosAreOrdinaryNumbers() {
            assertThat(simulate("PLACE -0,-0,NORTH REPORT").outputs()).containsExactly("0,0,NORTH");
            assertThat(simulate("PLACE 0004,00003,WEST REPORT").outputs()).containsExactly("4,3,WEST");
        }

        @Test
        void exactlyTheMaximumNumberOfCommandsIsAccepted() {
            assertThat(CommandParser.parse("MOVE ".repeat(CommandParser.MAX_COMMANDS))).hasSize(CommandParser.MAX_COMMANDS);
        }

        @Test
        void oneMoreThanTheMaximumIsRejected() {
            assertThatThrownBy(() -> CommandParser.parse("MOVE ".repeat(CommandParser.MAX_COMMANDS + 1)))
                    .isInstanceOf(TooManyCommandsException.class);
            List<String> lines = new ArrayList<>();
            for (int i = 0; i <= CommandParser.MAX_COMMANDS; i++) {
                lines.add("LEFT");
            }
            assertThatThrownBy(() -> CommandParser.parse(lines)).isInstanceOf(TooManyCommandsException.class);
        }

        @Test
        void tenThousandMovesAgainstAWallStayOnTheTable() {
            Simulator.Run run = simulate("PLACE 0,0,NORTH " + "MOVE ".repeat(9_998) + "REPORT");
            assertThat(run.outputs()).containsExactly("0,4,NORTH");
            assertThat(run.results().stream().filter(CommandResult::isIgnored)).hasSize(9_998 - 4);
        }
    }

    @Nested
    @DisplayName("Journeys")
    class Journeys {

        @Test
        void driveTheWholePerimeterPushingIntoEveryWall() {
            String lap = "MOVE MOVE MOVE MOVE MOVE MOVE RIGHT ";
            Simulator.Run run = simulate("PLACE 0,0,NORTH " + lap.repeat(4) + "REPORT");
            assertThat(run.outputs()).containsExactly("0,0,NORTH");
            assertThat(run.results().stream().filter(r -> r.reason() == Reason.WOULD_FALL)).hasSize(8);
        }

        @Test
        void serpentineVisitsAll25Cells() {
            List<String> visited = new ArrayList<>();
            StringBuilder commands = new StringBuilder("PLACE 0,0,EAST REPORT ");
            for (int row = 0; row < 5; row++) {
                commands.append("MOVE REPORT ".repeat(4));
                if (row < 4) {
                    String turn = row % 2 == 0 ? "LEFT" : "RIGHT";
                    commands.append(turn).append(" MOVE ").append(turn).append(" REPORT ");
                }
            }
            simulate(commands.toString()).outputs().forEach(o -> visited.add(o.substring(0, o.lastIndexOf(','))));
            assertThat(visited).doesNotHaveDuplicates().hasSize(25);
        }

        @Test
        void commandsBeforeAnyValidPlaceAreAllIgnored() {
            Simulator.Run run = simulate("REPORT MOVE LEFT RIGHT REPORT PLACE 5,5,NORTH PLACE -1,-1,SOUTH MOVE REPORT");
            assertThat(run.results()).allMatch(CommandResult::isIgnored);
            assertThat(run.outputs()).containsExactly("ROBOT MISSING", "ROBOT MISSING", "ROBOT MISSING");
        }

        @Test
        void replacingKeepsWorkingAndEmptyInputDoesNothing() {
            assertThat(simulate("PLACE 0,0,NORTH PLACE 4,4,SOUTH PLACE 2,2,EAST REPORT").outputs()).containsExactly("2,2,EAST");
            assertThat(simulate("").results()).isEmpty();
            assertThat(simulate(" \t\r\n ").results()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Parser abuse")
    class ParserAbuse {

        @ParameterizedTest(name = "accepted: [{0}]")
        @ValueSource(strings = {
                "place 1,2,east", "pLaCe 1,2,EaSt", "PLACE 1 , 2 , EAST", "PLACE\t1,\t2,\tEAST", "PLACE   1,2,EAST",
                "PLACE\n1,\n2,\nEAST", "  PLACE 1,2,EAST  ", "\r\nPLACE 1,2,EAST\r\n"})
        void lenientWhitespaceAndCase(String place) {
            assertThat(CommandParser.parse(place)).containsExactly(new Command.Place(1, 2, Direction.EAST));
        }

        @Test
        void allSeparatorsBetweenCommands() {
            assertThat(CommandParser.parse("MOVE\tLEFT\nRIGHT\r\nREPORT  MOVE")).hasSize(5);
        }

        @ParameterizedTest(name = "rejected: [{0}]")
        @ValueSource(strings = {
                "PLACE1,2,EAST", "PLACE 1,2,EAST,X", "PLACE 1;2;EAST", "PLACE 1 2 EAST", "PLACE +1,2,NORTH",
                "PLACE 1,2,NORTH-EAST", "PLACE 1,2,NORTHEAST", "PLACE 1,2,N", "PLACE 1,,EAST", "PLACE ,1,2",
                "PLACE 1,2,", "PLACE 1.0,2,EAST", "PLACE 1e2,2,EAST", "PLACE 0x1,2,EAST", "PLACE --1,2,EAST",
                "MOVE\u0000", "\u0000", "MOVEMOVE", "LEFTRIGHT", "REPORT2", "MOVE;", "JUMP", "move!", "PLACE",
                "ＭＯＶＥ", "PLACE ١,2,NORTH", "PLACE 1,2,ＮＯＲＴＨ", "M O V E", "MOVE LEFT", "'MOVE'", "\"MOVE\""})
        void garbageIsRejected(String input) {
            assertThatThrownBy(() -> CommandParser.parse(input)).isInstanceOf(CommandParseException.class);
        }

        @Test
        void oneBadTokenRejectsTheWholeBatch() {
            assertThatThrownBy(() -> CommandParser.parse("PLACE 0,0,NORTH MOVE MOVE BOOM REPORT"))
                    .isInstanceOf(CommandParseException.class).hasMessageContaining("BOOM");
        }

        @Test
        void parsingDoesNotDependOnTheDefaultLocale() {
            Locale original = Locale.getDefault();
            try {
                Locale.setDefault(Locale.forLanguageTag("tr-TR")); // "i".toUpperCase() is "İ" in Turkish
                assertThat(CommandParser.parse("right left place 1,1,west report")).hasSize(4);
                assertThat(Direction.parse("east")).contains(Direction.EAST);
            } finally {
                Locale.setDefault(original);
            }
        }
    }

    @Nested
    @DisplayName("Randomised against an independent reference model")
    class Fuzz {

        /** Deliberately naive re-implementation of the rules, sharing no code with the production classes. */
        private static List<String> reference(List<String> tokens) {
            int[] dx = {0, 1, 0, -1};
            int[] dy = {1, 0, -1, 0};
            Integer x = null;
            int y = 0;
            int f = 0;
            List<String> out = new ArrayList<>();
            for (String t : tokens) {
                if (t.startsWith("PLACE")) {
                    String[] p = t.substring(6).split(",");
                    int px = Integer.parseInt(p[0]);
                    int py = Integer.parseInt(p[1]);
                    if (px >= 0 && px <= 4 && py >= 0 && py <= 4) {
                        x = px;
                        y = py;
                        f = List.of(DIRS).indexOf(p[2]);
                    }
                } else if (x == null) {
                    if (t.equals("REPORT")) {
                        out.add("ROBOT MISSING");
                    }
                } else {
                    switch (t) {
                        case "MOVE" -> {
                            int nx = x + dx[f];
                            int ny = y + dy[f];
                            if (nx >= 0 && nx <= 4 && ny >= 0 && ny <= 4) {
                                x = nx;
                                y = ny;
                            }
                        }
                        case "LEFT" -> f = (f + 3) % 4;
                        case "RIGHT" -> f = (f + 1) % 4;
                        default -> out.add(x + "," + y + "," + DIRS[f]);
                    }
                }
            }
            return out;
        }

        @Test
        void twentyThousandRandomSessionsMatchTheReferenceAndNeverLeaveTheTable() {
            Random random = new Random(20261012L);
            String[] simple = {"MOVE", "MOVE", "MOVE", "LEFT", "RIGHT", "REPORT"};
            for (int session = 0; session < 20_000; session++) {
                List<String> tokens = new ArrayList<>();
                int length = random.nextInt(80);
                for (int i = 0; i < length; i++) {
                    tokens.add(random.nextInt(8) == 0
                            ? "PLACE %d,%d,%s".formatted(random.nextInt(9) - 2, random.nextInt(9) - 2, DIRS[random.nextInt(4)])
                            : simple[random.nextInt(simple.length)]);
                }
                tokens.add("REPORT");

                Position state = null;
                List<String> outputs = new ArrayList<>();
                for (Command command : CommandParser.parse(tokens)) {
                    Simulator.Step step = Simulator.execute(TABLE, state, command);
                    state = step.state();
                    if (state != null) {
                        assertThat(TABLE.contains(state)).as("session %d: %s", session, tokens).isTrue();
                    }
                    if (step.result().output() != null) {
                        outputs.add(step.result().output());
                    }
                }
                assertThat(outputs).as("session %d: %s", session, tokens).isEqualTo(reference(tokens));
            }
        }
    }
}
