package de.tostsoft.solarmonitoring.whatever.model;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document
public class ProxySolarSystem {
    @Id
    private String id;

    @NonNull
    private String token;

    private Set<Long> deyeSunSerials;
}
