package de.tostsoft.solarmonitoring.lib.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;

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
    private LocalDateTime validUntil;

    @DocumentReference(lazy = true)
    @NotNull
    private User ownedBy;
}
