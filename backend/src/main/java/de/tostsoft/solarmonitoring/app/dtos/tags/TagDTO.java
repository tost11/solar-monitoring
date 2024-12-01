package de.tostsoft.solarmonitoring.app.dtos.tags;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TagDTO {

    @NotNull
    private String id;

    @NotNull
    private String name;

    @NotNull
    private String color;

    @NotNull
    private Boolean locked;

}
