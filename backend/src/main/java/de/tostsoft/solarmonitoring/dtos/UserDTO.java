package de.tostsoft.solarmonitoring.dtos;

    import lombok.*;

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

        private int numbAllowedSystems;



    }

