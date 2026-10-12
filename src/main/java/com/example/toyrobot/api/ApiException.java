package com.example.toyrobot.api;

import com.example.toyrobot.constant.AppConstants.ErrorCodes;
import com.example.toyrobot.constant.AppConstants.ErrorMessages;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    static ApiException robotNotFound(String id) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.ROBOT_NOT_FOUND, ErrorMessages.ROBOT_NOT_FOUND.formatted(id));
    }
}
