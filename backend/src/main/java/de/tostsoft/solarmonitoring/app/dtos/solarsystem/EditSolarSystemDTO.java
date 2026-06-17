package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class EditSolarSystemDTO {

    private String id;
    private String token;

    @NotNull
    private String name;

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
}