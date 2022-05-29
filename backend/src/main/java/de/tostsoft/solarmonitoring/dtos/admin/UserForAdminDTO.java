package de.tostsoft.solarmonitoring.dtos.admin;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
public class UserForAdminDTO {
    @NonNull
    private long id;
    @NonNull
    private String name;

    private boolean isAdmin;

    private int numbAllowedSystems;

    private ZonedDateTime creationDate;

    private boolean isDeleted;
}

