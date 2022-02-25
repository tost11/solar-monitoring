package de.tostsoft.solarmonitoring.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GenericInfluxPoint {

    private float duration;
    private Long timestamp;
    private SolarSystemType type;
    private long systemId;

    public void copyTo(GenericInfluxPoint ret){
        ret.duration = duration;
        ret.timestamp = timestamp;
        ret.type = type;
        ret.systemId = systemId;
    }
}
