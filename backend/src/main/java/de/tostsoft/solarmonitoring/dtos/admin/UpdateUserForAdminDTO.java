package de.tostsoft.solarmonitoring.dtos.admin;

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
public class UpdateUserForAdminDTO {
    @NonNull
    private long id;
    @NonNull
    private String name;

    private boolean isAdmin;

    private int numAllowedSystems;

    private boolean isDeleted;
}

