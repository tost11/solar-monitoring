package de.tostsoft.solarmonitoring.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class GraphDTO {
    private List<Long> data;
    private List<Date> time;

}
