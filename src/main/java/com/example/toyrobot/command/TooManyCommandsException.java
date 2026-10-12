package com.example.toyrobot.command;

import com.example.toyrobot.constant.AppConstants.ErrorMessages;

public class TooManyCommandsException extends RuntimeException {

    public TooManyCommandsException(int limit) {
        super(ErrorMessages.TOO_MANY_COMMANDS.formatted(limit));
    }
}
