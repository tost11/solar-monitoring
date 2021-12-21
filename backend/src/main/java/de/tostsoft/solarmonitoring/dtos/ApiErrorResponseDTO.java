package de.tostsoft.solarmonitoring.dtos;

import java.time.ZonedDateTime;
import org.springframework.http.HttpStatus;

public class ApiErrorResponseDTO {

  private final String error;

  private final HttpStatus status;
  private final ZonedDateTime timestamp;

  public ApiErrorResponseDTO(String error, HttpStatus httpStatus, ZonedDateTime timeStep) {
    this.error = error;
    this.status = httpStatus;
    this.timestamp = timeStep;
  }

  public String getError() {
    return error;
  }

  public HttpStatus getHttpStatus() {
    return status;
  }

  public ZonedDateTime getTimeStep() {
    return timestamp;
  }
}
