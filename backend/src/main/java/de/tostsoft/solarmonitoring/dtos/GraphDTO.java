package de.tostsoft.solarmonitoring.dtos;

import de.tostsoft.solarmonitoring.model.Manages;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GraphDTO {
    private List<Double> data = new ArrayList<>();
    private List<Instant> time = new ArrayList<>();

    public void addTime(Instant time) {
        this.time.add(time);
    }
    public void addData(Double data) {
        this.data.add(data);
    }
}


