package com.example.toyrobot.api;

import com.example.toyrobot.command.CommandParseException;
import com.example.toyrobot.command.TooManyCommandsException;
import com.example.toyrobot.constant.AppConstants.ErrorCodes;
import com.example.toyrobot.constant.AppConstants.ErrorMessages;
import com.example.toyrobot.constant.AppConstants.LogMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return error(e.status(), e.code(), e.getMessage());
    }

    @ExceptionHandler(CommandParseException.class)
    ResponseEntity<ErrorResponse> handleParse(CommandParseException e) {
        return error(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_COMMAND, e.getMessage());
    }

    @ExceptionHandler(TooManyCommandsException.class)
    ResponseEntity<ErrorResponse> handleTooMany(TooManyCommandsException e) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCodes.TOO_MANY_COMMANDS, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof RequestSizeLimitFilter.BodyTooLargeException) {
                return error(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCodes.PAYLOAD_TOO_LARGE, cause.getMessage());
            }
        }
        return error(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_JSON, ErrorMessages.INVALID_JSON);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCodes.UNSUPPORTED_MEDIA_TYPE,
                ErrorMessages.UNSUPPORTED_MEDIA_TYPE.formatted(e.getSupportedMediaTypes()));
    }

    /** The client won't accept JSON, so there is no representation of the error we could send it. */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<Void> handleNotAcceptable(HttpMediaTypeNotAcceptableException e) {
        log.warn(LogMessages.CLIENT_ERROR, HttpStatus.NOT_ACCEPTABLE.value(), HttpStatus.NOT_ACCEPTABLE.name(), e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
        HttpHeaders headers = new HttpHeaders();
        if (e.getSupportedHttpMethods() != null) {
            headers.setAllow(e.getSupportedHttpMethods());
        }
        log.warn(LogMessages.CLIENT_ERROR, HttpStatus.METHOD_NOT_ALLOWED.value(), ErrorCodes.METHOD_NOT_ALLOWED, e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers)
                .body(ErrorResponse.of(ErrorCodes.METHOD_NOT_ALLOWED, e.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoRoute(NoResourceFoundException e) {
        return error(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, ErrorMessages.NO_ROUTE.formatted(e.getResourcePath()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error(LogMessages.UNEXPECTED_ERROR, e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR);
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        if (status.is4xxClientError()) {
            log.warn(LogMessages.CLIENT_ERROR, status.value(), code, message);
        }
        return ResponseEntity.status(status).body(ErrorResponse.of(code, message));
    }
}
