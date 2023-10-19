package de.tostsoft.solarmonitoring.app.dtos.solarsystem.data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class InputACDTO {

    @NotNull
    @Min(value = 0)
    protected Long id;

    @Min(value = 0)
    protected Float voltage;
    @Min(value = 0)
    protected Float ampere;
    @Min(value = 0)
    protected Float watt;

    @Min(value = 0)
    protected Float totalKWH;

    @Min(value = 0)
    protected Float frequency;
    @Min(value = 1)
    private Integer phase;
}
