package de.tostsoft.solarmonitoring.dtos.status;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllStatusResponseDTO {

    private List<BooleanStatusTDO> booleans;


}
