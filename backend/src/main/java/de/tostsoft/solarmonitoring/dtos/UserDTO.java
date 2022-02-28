package de.tostsoft.solarmonitoring.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

    @Getter
    @Setter
    @NoArgsConstructor
    @RequiredArgsConstructor
    public class UserDTO {
        @NonNull
        private long id;
        @NonNull
        private String name;

        private String jwt;

        private boolean isAdmin;

        private int numAllowedSystems;
    }

