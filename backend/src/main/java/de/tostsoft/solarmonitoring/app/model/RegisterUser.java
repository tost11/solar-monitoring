package de.tostsoft.solarmonitoring.app.model;

import com.mongodb.annotations.Sealed;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter
@Sealed
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterUser {

    @Id
    protected String id;

    @NotNull
    @Indexed(unique=true)
    protected String name;

    @NotNull
    protected String viewName;

    protected String mail;

    @NotNull
    protected String password;


}
