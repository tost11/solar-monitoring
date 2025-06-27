package de.tostsoft.solarmonitoring.lib.model;

import com.mongodb.annotations.Sealed;
import jakarta.validation.constraints.NotNull;
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
public class RegisterUser {

    @Id
    protected String id;

    @NotNull
    @Indexed(unique=true)
    protected String name;

    @NotNull
    protected String viewName;

    @NotNull
    @Indexed(unique=true)
    protected String mail;

    @NotNull
    protected Long createdAt;

    @NotNull
    protected String password;


}
