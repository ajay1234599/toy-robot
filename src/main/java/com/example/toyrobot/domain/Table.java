package com.example.toyrobot.domain;

import com.example.toyrobot.constant.AppConstants.ErrorMessages;
import com.example.toyrobot.constant.AppConstants.Limits;

public record Table(int width, int height) {

    public static final Table STANDARD = new Table(Limits.TABLE_WIDTH, Limits.TABLE_HEIGHT);

    public Table {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_TABLE);
        }
    }

    public boolean contains(long x, long y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean contains(Position position) {
        return contains(position.x(), position.y());
    }
}
