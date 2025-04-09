package de.tostsoft.solarmonitoring.app.dtos.tags;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class CreateTagDTO {
    private String id;

    @NotNull
    private String name;

    @NotNull
    private Boolean locked;

    @NotNull
    private String color;

    @NotNull
    private Boolean showOnStartPage;
}
