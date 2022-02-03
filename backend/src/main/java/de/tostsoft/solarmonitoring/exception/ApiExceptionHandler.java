package de.tostsoft.solarmonitoring.exception;


import de.tostsoft.solarmonitoring.dtos.ApiErrorResponseDTO;
import de.tostsoft.solarmonitoring.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.util.Date;

@ControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @ExceptionHandler(value = {ResponseStatusException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleHttpStatusException(ResponseStatusException e) {
        LOG.debug("responded with status code exception", e);
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                e.getReason(),
                e.getStatus(),
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, e.getStatus());
    }

    //is thrown by the authenticationProvider
    @ExceptionHandler(value = {BadCredentialsException.class, InternalAuthenticationServiceException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleBadCredentialsException(Exception e) {
        LOG.info("user tried to login in with bad credentials");
        HttpStatus badRequest = HttpStatus.BAD_REQUEST;
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                "invalid credentials",
                badRequest,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, badRequest);
    }

    //is thrown by the authenticationProvider
    @ExceptionHandler(value = {HttpRequestMethodNotSupportedException.class, MissingPathVariableException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleNotFoundException(Exception e) {
        LOG.debug("user tried to acces not existing endpoint");
        HttpStatus badRequest = HttpStatus.NOT_FOUND;
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                "endpoint not found",
                badRequest,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, badRequest);
    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<ApiErrorResponseDTO> handleException(Exception e) {
        LOG.error("caught unknown exception", e);
        HttpStatus badRequest = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                "internal server error... 'i just don't known what went wrong'",
                badRequest,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, badRequest);
    }


}