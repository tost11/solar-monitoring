package de.tostsoft.solarmonitoring.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ApiErrorResponseDTO {

    private String error;

    private HttpStatus status;
    private Date timestamp;




}
