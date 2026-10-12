package com.example.toyrobot.command;

import com.example.toyrobot.constant.AppConstants.Commands;
import com.example.toyrobot.constant.AppConstants.Formats;
import com.example.toyrobot.domain.Direction;

public sealed interface Command {

    String label();

    record Place(long x, long y, Direction facing) implements Command {
        @Override
        public String label() {
            return Formats.PLACE_LABEL.formatted(x, y, facing);
        }
    }

    record Move() implements Command {
        @Override
        public String label() {
            return Commands.MOVE;
        }
    }

    record Left() implements Command {
        @Override
        public String label() {
            return Commands.LEFT;
        }
    }

    record Right() implements Command {
        @Override
        public String label() {
            return Commands.RIGHT;
        }
    }

    record Report() implements Command {
        @Override
        public String label() {
            return Commands.REPORT;
        }
    }
}
