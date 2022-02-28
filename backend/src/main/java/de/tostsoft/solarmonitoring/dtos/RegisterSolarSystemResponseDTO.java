package de.tostsoft.solarmonitoring.dtos;

import de.tostsoft.solarmonitoring.model.SolarSystemType;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

//TODO split in two DTOs for creation and getting
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class RegisterSolarSystemResponseDTO {

    @NotNull
    private Long id;
    @NotNull
    private String token;
    @NotNull
    private String name;
    @NotNull
    private Date creationDate;
    private Date buildingDate;
    @NotNull
    private SolarSystemType type;

    private Double latitude;

    private Double longitude;
}