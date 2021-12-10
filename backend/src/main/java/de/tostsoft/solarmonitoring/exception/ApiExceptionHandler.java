package de.tostsoft.solarmonitoring.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(value = {ApiRequestException.class})
    public ResponseEntity<Object> handleApiRequestException(ApiRequestException e) {
        HttpStatus badRequest = HttpStatus.BAD_REQUEST;
        ApiException apiException = new ApiException(
                e.getMessage(),
                badRequest,
                ZonedDateTime.now(ZoneId.of("Europe/Berlin"))
        );
        return new ResponseEntity<>(apiException, badRequest);
    }

    @ExceptionHandler(value = {InternalServerException.class})
    public ResponseEntity<Object> handleInternalServerException(InternalServerException e) {
        HttpStatus internalServerError = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiException apiException = new ApiException(
                e.getMessage(),
                internalServerError,
                ZonedDateTime.now(ZoneId.of("Europe/Berlin"))
        );
        return new ResponseEntity<>(apiException, internalServerError);
    }

    @ExceptionHandler(value = {AuthenticationError.class})
    public ResponseEntity<Object> handleAuthenticationException(AuthenticationError e) {
        HttpStatus forbidden = HttpStatus.FORBIDDEN;
        ApiException apiException = new ApiException(
                e.getMessage(),
                forbidden,
                ZonedDateTime.now(ZoneId.of("Europe/Berlin"))
        );
        return new ResponseEntity<>(apiException, forbidden);
    }

    @ExceptionHandler(value = {AuthenticationError.class})
    public ResponseEntity<Object> handleNotFoundxception(AuthenticationError e) {
        HttpStatus notFound = HttpStatus.NOT_FOUND;
        ApiException apiException = new ApiException(
                e.getMessage(),
                notFound,
                ZonedDateTime.now(ZoneId.of("Europe/Berlin"))
        );
        return new ResponseEntity<>(apiException, notFound);
    }

    @ExceptionHandler(value = {UnAuthorizedError.class})
    public ResponseEntity<Object> handleUserNotFound(UnAuthorizedError e) {
        HttpStatus unauthorized = HttpStatus.UNAUTHORIZED;
        ApiException apiException = new ApiException(
                e.getMessage(),
                unauthorized,
                ZonedDateTime.now(ZoneId.of("Europe/Berlin"))
        );
        return new ResponseEntity<>(apiException, unauthorized);
    }
}