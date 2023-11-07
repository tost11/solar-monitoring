package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.app.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.app.dtos.status.AllStatusResponseDTO;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SolarSystemDTO {

    @NotNull
    private String id;

    @NotNull
    private String name;
    @NotNull
    private String viewName;

    private String shortener;

    @NotNull
    private ZonedDateTime creationDate;
    private ZonedDateTime buildingDate;
    @NotNull
    private SolarSystemType type;

    private Double latitude;

    private Double longitude;

    @NotNull
    private NamingsDTO namings;

    @NotNull
    private ViewDataDTO viewData;

    private Float electricityPrice;

    private TotalValuesDTO totalValuesDTO;

    private PublicMode publicMode;

    private List<ManagerDTO> managers;

    private AllStatusResponseDTO status;

    private Boolean publicFlagOnlyProduction;

    @NotNull
    private String timezone;

    private String deyeSunSerialNumbers;
}