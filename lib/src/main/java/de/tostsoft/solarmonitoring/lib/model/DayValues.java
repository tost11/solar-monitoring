package de.tostsoft.solarmonitoring.lib.model;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DayValues {
    Float producedKWH;
    Float consumedKWH;
    Float producedKWHPrice;
    Float consumedKWHPrice;
    Float gridConsumedKWH;
    Float gridConsumedKWHPrice;
    Float gridFeedInKWH;
    Float gridFeedInKWHPrice;
    Float calcConsumedKWH;
    Float calcConsumedKWHPrice;
    LocalDate localDate;
}
