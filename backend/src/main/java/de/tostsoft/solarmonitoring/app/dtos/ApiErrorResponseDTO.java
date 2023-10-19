package de.tostsoft.solarmonitoring.app.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatusCode;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ApiErrorResponseDTO {

    private String error;

    private HttpStatusCode status;

    private Date timestamp;
}
