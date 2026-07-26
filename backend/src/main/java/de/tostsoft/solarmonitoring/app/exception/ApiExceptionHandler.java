package de.tostsoft.solarmonitoring.app.exception;


import de.tostsoft.solarmonitoring.lib.dtos.ApiErrorResponseDTO;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Date;

@ControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @PostConstruct
    private void init(){
        LOG.debug("Debug logging enabled for ApiExceptionHandler");
    }

    @ExceptionHandler(value = {ResponseStatusException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleHttpStatusException(ResponseStatusException e) {
        LOG.debug("responded with status code exception", e);
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                e.getReason(),
                e.getStatusCode(),
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, e.getStatusCode());
    }

    @ExceptionHandler(value = {NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleHttpStatusException(NoResourceFoundException e) {
        LOG.debug("responded with status code exception", e);
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                "Resource not found",
                HttpStatus.NOT_FOUND,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, e.getStatusCode());
    }

    @ExceptionHandler(value = {HttpMessageNotReadableException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleHttpStatusException(HttpMessageNotReadableException e) {
        LOG.error("responded with status code exception", e);
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                "Request body is missing or invalid",
                HttpStatus.BAD_REQUEST,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, HttpStatus.BAD_REQUEST);
    }

    //is thrown by the authenticationProvider
    @ExceptionHandler(value = {BadCredentialsException.class, InternalAuthenticationServiceException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleBadCredentialsException(Exception e) {
        LOG.info("user tried to login in with bad credentials");
        HttpStatus badRequest = HttpStatus.UNAUTHORIZED;
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                "Invalid username or password",
                badRequest,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, badRequest);
    }

    //is thrown by the authenticationProvider
    @ExceptionHandler(value = {LockedException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleLockedException(Exception e) {
        LOG.info("user tried to login in with deleted account");
        HttpStatus badRequest = HttpStatus.UNAUTHORIZED;
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                "Invalid username or password",
                badRequest,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, badRequest);
    }

    //is thrown by the authenticationProvider
    @ExceptionHandler(value = {DisabledException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleDisabledException(Exception e) {
        LOG.info("user tried to login with not activated account");
        HttpStatus badRequest = HttpStatus.UNAUTHORIZED;
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                "Invalid username or password",
                badRequest,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, badRequest);
    }

    @ExceptionHandler(value = {HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleMethodNotFoundException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        LOG.debug("endpoint called with wrong Method: {} {}", request.getMethod(), request.getRequestURI(), e);
        HttpStatus badRequest = HttpStatus.BAD_REQUEST;
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                "methode dose not match requirements",
                badRequest,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, badRequest);
    }

    @ExceptionHandler(value = {MissingPathVariableException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleNotFoundException(MissingPathVariableException e) {
        LOG.debug("endpoint called with missingPathVariable",e);
        HttpStatus badRequest = HttpStatus.NOT_FOUND;
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                "required path variable missing: '"+e.getVariableName()+"'",
                badRequest,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, badRequest);
    }


    //is thrown by the authenticationProvider
    @ExceptionHandler(value = {MissingServletRequestParameterException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleNotMissingServletParameter(Exception e) {
        LOG.debug("user tried to call endpoint with wrong servlet parameter");
        HttpStatus badRequest = HttpStatus.BAD_REQUEST;
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                e.getMessage(),
                badRequest,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, badRequest);
    }

    //is thrown by the authenticationProvider
    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ResponseEntity<ApiErrorResponseDTO> MethodArgumentNotValidException(MethodArgumentNotValidException e) {
        LOG.info("received invalid request because some paremters are not ok",e);

        String errorText = "some request paramters are invalid";

        try{
            String generatedError = "Invalid request because of: ";
            boolean first = true;
            for (final FieldError error : e.getBindingResult().getFieldErrors()) {
                if(first){
                    first = false;
                }else{
                    generatedError+=" ,";
                }
                generatedError += error.getField() + " <- " + error.getDefaultMessage();
            }
            for (final ObjectError error : e.getBindingResult().getGlobalErrors()) {
                if(first){
                    first = false;
                }else{
                    generatedError += " ,";
                }
                generatedError += error.getObjectName() + " <- " + error.getDefaultMessage();
            }
            errorText = generatedError;
        }catch (Exception ex){
            LOG.error("Error on parsing validation errors",ex);
        }

        HttpStatus badRequest = HttpStatus.BAD_REQUEST;
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
            errorText,
            badRequest,
            new Date());

        return new ResponseEntity<>(apiErrorResponseDTO, badRequest);
    }
    //is thrown by the authenticationProvider
    @ExceptionHandler(value = {MissingRequestHeaderException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleNotMissingHeaderParameter(MissingRequestHeaderException e) {
        LOG.debug("user tried to call endpoint with missing header paramter: {}",e.getHeaderName());
        HttpStatus badRequest = HttpStatus.BAD_REQUEST;
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                e.getMessage(),
                badRequest,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, badRequest);
    }


    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<ApiErrorResponseDTO> handleException(Exception e) {
        LOG.error("caught unknown exception", e);
        LOG.debug("caught unknown exception", e.getMessage(),e);
        HttpStatus errorCode = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                "internal server error... 'i just don't known what went wrong'",
                errorCode,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, errorCode);
    }


}