package de.tostsoft.solarmonitoring.lib.model;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TotalValues {
    Float producedKWH;
    Float consumedKWH;
    Float producedKWHPrice;
    Float consumedKWHPrice;
    Float gridConsumedKWH;
    Float gridConsumedKWHPrice;
    Float gridFeedInKWH;
    Float gridFeedInKWHPrice;
}
