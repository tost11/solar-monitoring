package de.tostsoft.solarmonitoring.app.model;

import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MultSolarDataWrapper {

    Map<LocalDate, AtomicLong> currentSamplesDay = new HashMap<>();

    List<SolarSampleWrapper> samples = new ArrayList<>();
}
