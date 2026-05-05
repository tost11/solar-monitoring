package de.tostsoft.solarmonitoring.lib.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document
public class JWTSessionToken {

    @Id
    private String id;

    @NonNull
    @Indexed(expireAfterSeconds = 0)
    private Instant validUntil;

    @DocumentReference(lazy = true)
    @NotNull
    private User ownedBy;
}
