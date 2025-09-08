package de.tostsoft.solarmonitoring.proxy.dtos;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class MultDataResponseProxyDTO {
    List<Integer> invalidSamplesIndexes = new ArrayList<>();
}
