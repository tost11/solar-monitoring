package de.tostsoft.solarmonitoring.dtos.solarsystem.data;

import lombok.*;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class BaseOutputDTO {

    @NotNull
    protected Long id;

    protected Float voltage;
    protected Float ampere;
    protected Float watt;

    protected Float frequency;
}
