package de.tostsoft.solarmonitoring.exception;


import de.tostsoft.solarmonitoring.dtos.ApiErrorResponseDTO;
import de.tostsoft.solarmonitoring.service.UserService;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

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

    //is thrown by the authenticationProvider
    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ResponseEntity<ApiErrorResponseDTO> MethodArgumentNotValidException(MethodArgumentNotValidException e) {
        LOG.debug("received invalid request because some paremters are not ok",e);

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

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<ApiErrorResponseDTO> handleException(Exception e) {
        LOG.error("caught unknown exception", e);
        HttpStatus badRequest = HttpStatus.BAD_REQUEST;
        ApiErrorResponseDTO apiErrorResponseDTO = new ApiErrorResponseDTO(
                "internal server error... 'i just don't known what went wrong'",
                badRequest,
                new Date());
        return new ResponseEntity<>(apiErrorResponseDTO, badRequest);
    }


}