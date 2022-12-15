package de.tostsoft.solarmonitoring.dtos.solarsystem.data;

import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class OutputDTO {

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
    protected Float frequency;
    @Min(value = 1)
    private Integer phase;
}
