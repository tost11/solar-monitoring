package de.tostsoft.solarmonitoring.lib.model;

import com.mongodb.annotations.Sealed;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document
public class RegisterUser implements UserDetails {

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
    @Indexed(expireAfterSeconds = 86400)
    protected Instant createdAt;

    @NotNull
    protected String password;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return name;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
