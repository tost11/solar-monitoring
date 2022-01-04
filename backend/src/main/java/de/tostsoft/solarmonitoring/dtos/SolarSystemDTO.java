package de.tostsoft.solarmonitoring.dtos;

import de.tostsoft.solarmonitoring.model.SolarSystemType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

//TODO split in two DTOs for creation and getting
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
    private SolarSystemType type;

    private Float latitude;

    private Float longitude;
}