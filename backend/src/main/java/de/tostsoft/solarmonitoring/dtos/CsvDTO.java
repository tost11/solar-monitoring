package de.tostsoft.solarmonitoring.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CsvDTO {
    private long systemId;
    private String field;
    private String from;
    private String to;
}
