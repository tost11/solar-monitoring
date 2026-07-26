package de.tostsoft.solarmonitoring.proxy.model;

import de.tostsoft.solarmonitoring.lib.model.AccessToken;
import java.util.List;
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

    private Set<Long> deyeSunSerials;

    private List<AccessToken> tokens;

    @NonNull
    private Long lastUpdate;
}
