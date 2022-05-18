package de.tostsoft.solarmonitoring.dtos.solarsystem;

import de.tostsoft.solarmonitoring.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.model.enums.SolarSystemType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

//TODO split in two DTOs for creation and getting
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolarSystemDTO {

    @NotNull
    private Long id;

    @NotNull
    private String name;

    @NotNull
    private LocalDateTime creationDate;
    private LocalDateTime buildingDate;
    @NotNull
    private SolarSystemType type;

    private Double latitude;

    private Double longitude;

    private Boolean isBatteryPercentage;

    private Integer inverterVoltage;

    private Integer batteryVoltage;
    private Integer maxSolarVoltage;

    private List<ManagerDTO> managers;

    @NotNull
    private String timezone;
}