package de.tostsoft.solarmonitoring.lib.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document
public class Captcha {

    @Id
    private String id;

    @Indexed(unique = true,sparse = true)
    private String base64Image;

    private String text;

    private Long createdAt;
}
