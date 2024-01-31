package de.tostsoft.solarmonitoring.lib.model;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TotalValues {
    Float producedKWH;
    Float calcProducedKWH;
    Float consumedKWH;
    Float calcConsumedKWH;
    Float producedKWHPrice;
    Float calcProducedKWHPrice;
    Float consumedKWHPrice;
    Float calcConsumedKWHPrice;
}
