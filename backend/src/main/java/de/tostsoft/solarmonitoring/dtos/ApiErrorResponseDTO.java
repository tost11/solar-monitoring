package de.tostsoft.solarmonitoring.dtos;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

public class ApiErrorResponseDTO {

    private final String error;

    private final HttpStatus status;
    private final ZonedDateTime timestamp;

    public ApiErrorResponseDTO(String error, HttpStatus httpStatus, ZonedDateTime timestamp) {
        this.error = error;
        this.status = httpStatus;
        this.timestamp = timestamp;
    }

    public String getError() {
        return error;
    }

    public HttpStatus getHttpStatus() {
        return status;
    }

    public ZonedDateTime getTimeStamp() {
        return timestamp;
    }
}
