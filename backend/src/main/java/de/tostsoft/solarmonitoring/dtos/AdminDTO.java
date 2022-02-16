package de.tostsoft.solarmonitoring.dtos;

    import lombok.*;

    @Getter
    @Setter
    @NoArgsConstructor
    @RequiredArgsConstructor
    public class UserDTO {
        @NonNull
        private String name;

        private String jwt;


    }

