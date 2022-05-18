package de.tostsoft.solarmonitoring.dtos.solarsystem;

import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import java.util.Date;
import java.util.TimeZone;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//TODO split in two DTOs for creation and getting
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
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

    @NotNull
    private String timezone;
}