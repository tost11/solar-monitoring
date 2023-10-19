package de.tostsoft.solarmonitoring.app.dtos.solarsystem;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TotalValuesDTO {
    Float producedKWH;
    Float calcProducedKWH;
    Float consumedKWH;
    Float calcConsumedKWH;
    Float earnedMoney;
}