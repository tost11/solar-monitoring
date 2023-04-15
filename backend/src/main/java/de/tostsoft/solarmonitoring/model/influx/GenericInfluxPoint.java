package de.tostsoft.solarmonitoring.model.influx;

import de.tostsoft.solarmonitoring.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class GenericInfluxPoint {

    protected float duration;
    protected Long timestamp;
    protected SolarSystemType type;
    protected String systemId;

    public abstract InfluxMeasurement getMeasurement();
}
