package de.tostsoft.solarmonitoring.app.model;

import de.tostsoft.solarmonitoring.lib.dtos.solarsystem.data.SampleDTO;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SolarSampleWrapper {

    SampleDTO sample;

    boolean valid;

    boolean limitReached;
}
