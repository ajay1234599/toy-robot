package com.example.toyrobot.domain;

import com.example.toyrobot.constant.AppConstants.ErrorMessages;
import com.example.toyrobot.constant.AppConstants.Formats;
import java.util.Objects;

public record Position(int x, int y, Direction facing) {

    public Position {
        Objects.requireNonNull(facing, ErrorMessages.FACING_REQUIRED);
    }

    public Position forward() {
        return new Position(x + facing.dx(), y + facing.dy(), facing);
    }

    public Position turnedLeft() {
        return new Position(x, y, facing.left());
    }

    public Position turnedRight() {
        return new Position(x, y, facing.right());
    }

    public String report() {
        return Formats.POSITION.formatted(x, y, facing);
    }
}
