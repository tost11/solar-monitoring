package de.tostsoft.solarmonitoring.app.dtos;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class MultDataResponseDTO {
    List<Integer> invalidSamplesIndexes = new ArrayList<>();
    List<Integer> dailyLimitReachedIndexes = new ArrayList<>();
}
