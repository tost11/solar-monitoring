package de.tostsoft.solarmonitoring.lib.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TagDTO {
    private String id;
    private String name;
    private String color;
}
