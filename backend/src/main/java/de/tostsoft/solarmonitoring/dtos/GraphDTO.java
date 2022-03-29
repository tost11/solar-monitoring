package de.tostsoft.solarmonitoring.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.Map;
@Getter
@Setter
@NoArgsConstructor
public class GraphDTO {
private List<Date> time ;
private Map<String,List<Double>> data;
}
