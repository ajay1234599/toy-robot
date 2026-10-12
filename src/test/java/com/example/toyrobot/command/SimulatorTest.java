package com.example.toyrobot.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.toyrobot.command.CommandResult.Reason;
import com.example.toyrobot.command.CommandResult.Status;
import com.example.toyrobot.domain.Direction;
import com.example.toyrobot.domain.Position;
import com.example.toyrobot.domain.Table;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;

class SimulatorTest {

    private static Simulator.Run simulate(String input) {
        return Simulator.run(Table.STANDARD, null, CommandParser.parse(input));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "PLACE 0,0,NORTH MOVE REPORT                | 0,1,NORTH",
            "PLACE 0,0,NORTH LEFT REPORT                | 0,0,WEST",
            "PLACE 1,2,EAST MOVE MOVE LEFT MOVE REPORT  | 3,3,NORTH",
            "MOVE REPORT                                | ROBOT MISSING",
    })
    void specificationExamples(String commands, String expected) {
        assertThat(simulate(commands).outputs()).containsExactly(expected);
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "0,-1", "5,0", "0,5", "10,10"})
    void placementOffTheTableIsIgnored(int x, int y) {
        Simulator.Run run = Simulator.run(Table.STANDARD, null, List.of(new Command.Place(x, y, Direction.NORTH)));
        assertThat(run.state()).isNull();
        assertThat(run.results().getFirst().reason()).isEqualTo(Reason.OFF_TABLE);
    }

    @Test
    void offTablePlacementKeepsExistingPosition() {
        assertThat(simulate("PLACE 2,2,NORTH PLACE 5,5,SOUTH REPORT").outputs()).containsExactly("2,2,NORTH");
    }

    @Test
    void moveThatWouldFallIsIgnoredButLaterCommandsRun() {
        Simulator.Run run = simulate("PLACE 0,0,SOUTH MOVE LEFT MOVE REPORT");
        assertThat(run.results().get(1).status()).isEqualTo(Status.IGNORED);
        assertThat(run.results().get(1).reason()).isEqualTo(Reason.WOULD_FALL);
        assertThat(run.state()).isEqualTo(new Position(1, 0, Direction.EAST));
    }

    @ParameterizedTest
    @ValueSource(strings = {"4,4,NORTH", "4,4,EAST", "0,0,SOUTH", "0,0,WEST"})
    void robotCannotLeaveAnyEdge(String placement) {
        assertThat(simulate("PLACE " + placement + " MOVE MOVE REPORT").outputs()).containsExactly(placement);
    }

    @Test
    void laterPlaceRepositionsTheRobot() {
        assertThat(simulate("PLACE 0,0,NORTH REPORT PLACE 3,2,WEST REPORT").outputs())
                .containsExactly("0,0,NORTH", "3,2,WEST");
    }

    @Test
    void commandsBeforeFirstValidPlaceAreIgnored() {
        Simulator.Run run = simulate("MOVE LEFT RIGHT PLACE 9,9,NORTH MOVE PLACE 2,2,NORTH MOVE REPORT");
        assertThat(run.results().subList(0, 5)).allMatch(CommandResult::isIgnored);
        assertThat(run.results().subList(0, 5)).extracting(CommandResult::reason)
                .containsExactly(Reason.NOT_PLACED, Reason.NOT_PLACED, Reason.NOT_PLACED, Reason.OFF_TABLE,
                        Reason.NOT_PLACED);
        assertThat(run.outputs()).containsExactly("2,3,NORTH");
    }

    @Test
    void robotCanTraverseTheWholeTable() {
        assertThat(simulate("PLACE 0,0,EAST MOVE MOVE MOVE MOVE MOVE LEFT MOVE MOVE MOVE MOVE MOVE REPORT").outputs())
                .containsExactly("4,4,NORTH");
    }
}
