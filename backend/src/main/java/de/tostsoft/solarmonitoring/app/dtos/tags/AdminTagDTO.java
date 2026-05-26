package de.tostsoft.solarmonitoring.app.dtos.tags;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class AdminTagDTO extends TagDTO {

    @NotNull
    private Boolean locked;

    @NotNull
    private Boolean showOnStartPage;

}
