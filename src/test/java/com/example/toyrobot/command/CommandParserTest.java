package com.example.toyrobot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.toyrobot.domain.Direction;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommandParserTest {

    @Test
    void parsesAllCommands() {
        assertThat(CommandParser.parse("PLACE 1,2,EAST MOVE LEFT RIGHT REPORT")).containsExactly(
                new Command.Place(1, 2, Direction.EAST),
                new Command.Move(),
                new Command.Left(),
                new Command.Right(),
                new Command.Report());
    }

    @Test
    void isCaseInsensitiveAndToleratesSpacesInPlaceArguments() {
        assertThat(CommandParser.parse("place 1, 2 ,east\nmove"))
                .containsExactly(new Command.Place(1, 2, Direction.EAST), new Command.Move());
    }

    @Test
    void parsesListOfLines() {
        assertThat(CommandParser.parse(List.of("PLACE 0,0,NORTH", "REPORT")))
                .containsExactly(new Command.Place(0, 0, Direction.NORTH), new Command.Report());
    }

    @Test
    void emptyInputIsNoCommands() {
        assertThat(CommandParser.parse("   ")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"JUMP", "PLACE", "PLACE 1,2", "PLACE 1,2,UP", "PLACE 1.5,2,NORTH", "PLACE a,b,NORTH"})
    void rejectsMalformedInput(String input) {
        assertThatThrownBy(() -> CommandParser.parse(input)).isInstanceOf(CommandParseException.class);
    }
}
