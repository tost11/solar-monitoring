package de.tostsoft.solarmonitoring.app.dtos.status;

import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BooleanStatusTDO {
    private String name;
    private ZonedDateTime lastSet;
    private boolean value;
}
