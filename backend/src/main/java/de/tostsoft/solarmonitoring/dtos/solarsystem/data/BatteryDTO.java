package de.tostsoft.solarmonitoring.dtos.solarsystem.data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class BatteryDTO {

    @NotNull
    @Min(value = 0)
    protected Long id;

    @Min(value = 0)
    protected Float voltage;
    protected Float ampere;
    protected Float watt;

    protected Float totalKWH;
}
