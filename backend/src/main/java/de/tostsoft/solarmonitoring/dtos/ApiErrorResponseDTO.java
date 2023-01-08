package de.tostsoft.solarmonitoring.dtos;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ApiErrorResponseDTO {

    private String error;

    private HttpStatusCode status;

    private Date timestamp;
}
