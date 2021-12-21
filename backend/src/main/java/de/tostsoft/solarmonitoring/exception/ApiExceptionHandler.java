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
        ApiErrorResponse apiErrorResponse = new ApiErrorResponse(
                e.getMessage(),
                badRequest,
                ZonedDateTime.now(ZoneId.of("Europe/Berlin"))
        );
        return new ResponseEntity<>(apiErrorResponse, badRequest);
    }

    @ExceptionHandler(value = {InternalServerException.class})
    public ResponseEntity<Object> handleInternalServerException(InternalServerException e) {
        HttpStatus internalServerError = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiErrorResponse apiErrorResponse = new ApiErrorResponse(
                e.getMessage(),
                internalServerError,
                ZonedDateTime.now(ZoneId.of("Europe/Berlin"))
        );
        return new ResponseEntity<>(apiErrorResponse, internalServerError);
    }

    @ExceptionHandler(value = {AuthenticationError.class})
    public ResponseEntity<Object> handleAuthenticationException(AuthenticationError e) {
        HttpStatus forbidden = HttpStatus.FORBIDDEN;
        ApiErrorResponse apiErrorResponse = new ApiErrorResponse(
                e.getMessage(),
                forbidden,
                ZonedDateTime.now(ZoneId.of("Europe/Berlin"))
        );
        return new ResponseEntity<>(apiErrorResponse, forbidden);
    }

    @ExceptionHandler(value = {NotFoundException.class})
    public ResponseEntity<Object> handleNotFoundException(NotFoundException e) {
        HttpStatus notFound = HttpStatus.NOT_FOUND;
        ApiErrorResponse apiErrorResponse = new ApiErrorResponse(
                e.getMessage(),
                notFound,
                ZonedDateTime.now(ZoneId.of("Europe/Berlin"))
        );
        return new ResponseEntity<>(apiErrorResponse, notFound);
    }

    @ExceptionHandler(value = {UnAuthorizedError.class})
    public ResponseEntity<Object> handleUserNotFound(UnAuthorizedError e) {
        HttpStatus unauthorized = HttpStatus.UNAUTHORIZED;
        ApiErrorResponse apiErrorResponse = new ApiErrorResponse(
                e.getMessage(),
                unauthorized,
                ZonedDateTime.now(ZoneId.of("Europe/Berlin"))
        );
        return new ResponseEntity<>(apiErrorResponse, unauthorized);
    }
}