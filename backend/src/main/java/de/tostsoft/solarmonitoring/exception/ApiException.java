package de.tostsoft.solarmonitoring.exception;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

public class ApiException {
    private final String message;

    private final HttpStatus httpStatus;
    private final ZonedDateTime timeStep;

    public ApiException(String message, HttpStatus httpStatus, ZonedDateTime timeStep) {
        this.message = message;
        this.httpStatus = httpStatus;
        this.timeStep = timeStep;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public ZonedDateTime getTimeStep() {
        return timeStep;
    }
}
