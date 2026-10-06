package de.tostsoft.solarmonitoring.lib.model;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemCurves {
    LocalDate localDate;
    Float[] productionCurve;
}
