package de.tostsoft.solarmonitoring.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class SolarSystemDTO {

    private Long id;

    private String token;
    @NotNull
    private String name;
    @NotNull
    private Long creationDate;
    @NotNull
    private String type;

    private Float latitude;

    private Float longitude;

}