package de.tostsoft.solarmonitoring.lib.model.influx;

import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import lombok.experimental.SuperBuilder;

import lombok.*;

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
