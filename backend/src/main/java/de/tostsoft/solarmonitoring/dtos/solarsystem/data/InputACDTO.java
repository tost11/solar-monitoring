package de.tostsoft.solarmonitoring.dtos.solarsystem.data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    protected Float frequency;
    @Min(value = 1)
    private Integer phase;
}
