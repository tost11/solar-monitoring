package de.tostsoft.solarmonitoring.dtos;

import de.tostsoft.solarmonitoring.model.SolarSystemType;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import jdk.jfr.Label;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Id;

//TODO split in two DTOs for creation and getting
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class SolarSystemDTO {

    @NotNull
    private Long id;

    @NotNull
    private String name;

    @NotNull
    private Date creationDate;
    private Date buildingDate;
    @NotNull
    private SolarSystemType type;

    private Float latitude;

    private Float longitude;

    private Boolean isBatteryPercentage;

    private Float inverterVoltage;

    private Float batteryVoltage;
    private Float maxSolarVoltage;

    private List<ManagerDTO> managers;


}