package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.app.dtos.ManagerDTO;
import de.tostsoft.solarmonitoring.app.dtos.status.AllStatusResponseDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.TagDTO;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class EditSolarSystemDTO {

    private String id;
    private String token;

    @Size(max=1000)
    private String shortener;

    @NotNull
    private SolarSystemType type;

    @NotNull
    @Valid
    private SystemInformationsDTO systemInformations;

    @NotNull
    @Valid
    private ViewDataDTO viewData;

    @NotNull
    private String timezone;

    @NotNull
    private PublicMode publicMode;

    @NotNull
    @Valid
    private NamingsDTO namings;

    @Size(max=1000)
    private String deyeSunSerialNumbers;

    private Boolean calculateCombinedValuesAfterwards;

    private AllStatusResponseDTO status;

    //not notnull, for pushing data it is fine to be empty its separate calls
    private List<TagDTO> tags;

    //not notnull, for pushing data it is fine to be empty its separate calls
    private List<ManagerDTO> managers;

    //only populated for owners, null for non-owners
    private List<AccessTokenResponseDTO> tokens;
}
