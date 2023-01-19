package de.tostsoft.solarmonitoring.dtos.status;

import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BooleanStatus {
    private String name;
    private ZonedDateTime lastSet;
    private boolean value;
}
