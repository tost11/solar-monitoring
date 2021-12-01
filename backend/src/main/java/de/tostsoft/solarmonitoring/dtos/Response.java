package de.tostsoft.solarmonitoring.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class Response {
    @NonNull
    private String message;
}
