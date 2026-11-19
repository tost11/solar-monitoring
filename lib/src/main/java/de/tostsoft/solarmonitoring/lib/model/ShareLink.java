package de.tostsoft.solarmonitoring.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "shareLinks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLink {

    @Id
    private String id;

    @Indexed
    private String systemId;

    private String secretHash;

    private boolean manual;

    private String notes;

    private String level;

    private Integer remainingUsages;

    private LocalDateTime expiresAt;

    private LocalDateTime tokenExpiresAt;

    private boolean revoked;

    private LocalDateTime createdAt;

    private LocalDateTime lastRedeemedAt;
}
