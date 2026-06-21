package de.tostsoft.solarmonitoring.lib.model;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Tag {

    @Id
    private String id;

    @NonNull
    @Indexed(unique=true)
    private String name;

    @NonNull
    private String viewName;

    @NonNull
    private Boolean locked;

    @NonNull
    private String color;

    @NonNull
    private Boolean showOnStartPage;

    private Boolean showStartPageAggregation;
}
