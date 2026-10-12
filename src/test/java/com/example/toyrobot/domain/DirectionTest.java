package com.example.toyrobot.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DirectionTest {

    @Test
    void rightRotatesClockwise() {
        assertThat(Direction.NORTH.right()).isEqualTo(Direction.EAST);
        assertThat(Direction.EAST.right()).isEqualTo(Direction.SOUTH);
        assertThat(Direction.SOUTH.right()).isEqualTo(Direction.WEST);
        assertThat(Direction.WEST.right()).isEqualTo(Direction.NORTH);
    }

    @Test
    void leftRotatesAntiClockwise() {
        assertThat(Direction.NORTH.left()).isEqualTo(Direction.WEST);
        assertThat(Direction.WEST.left()).isEqualTo(Direction.SOUTH);
        assertThat(Direction.SOUTH.left()).isEqualTo(Direction.EAST);
        assertThat(Direction.EAST.left()).isEqualTo(Direction.NORTH);
    }

    @Test
    void rotationDoesNotChangePosition() {
        Position position = new Position(2, 3, Direction.NORTH);
        assertThat(position.turnedLeft()).isEqualTo(new Position(2, 3, Direction.WEST));
        assertThat(position.turnedRight()).isEqualTo(new Position(2, 3, Direction.EAST));
    }

    @Test
    void parseIsCaseInsensitive() {
        assertThat(Direction.parse(" south ")).contains(Direction.SOUTH);
        assertThat(Direction.parse("UP")).isEmpty();
        assertThat(Direction.parse(null)).isEmpty();
    }

    @Test
    void tableBoundsAreZeroToFour() {
        Table table = Table.STANDARD;
        assertThat(table.contains(0, 0)).isTrue();
        assertThat(table.contains(4, 4)).isTrue();
        assertThat(table.contains(5, 4)).isFalse();
        assertThat(table.contains(-1, 0)).isFalse();
    }
}
