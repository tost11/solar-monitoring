package de.tostsoft.solarmonitoring.model.influx;

import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import lombok.*;

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
