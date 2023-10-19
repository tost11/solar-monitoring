package de.tostsoft.solarmonitoring.app.dtos.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
public class UpdateUserForAdminDTO {
    @NonNull
    private String id;
    @NonNull
    private String name;

    private boolean admin;

    private int numAllowedSystems;

    private boolean isDeleted;
}

