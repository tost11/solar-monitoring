package de.tostsoft.solarmonitoring.app.dtos.tags;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class TagDTO {

    @NotNull
    private String id;

    @NotNull
    private String name;

    @NotNull
    private String color;
}
