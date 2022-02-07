package de.tostsoft.solarmonitoring.model;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Node("User")
public class User implements UserDetails {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    private String name;

    private String password;

    @NotNull
    private Boolean initialisationFinished;

    @NotNull
    private Instant creationDate;

    private Long grafanaUserId;
    private Long grafanaFolderId;

    private boolean isAdmin = false;

    @Lazy
    @Relationship(type = "owns", direction = Relationship.Direction.OUTGOING)
    private List<SolarSystem> relationOwns;

    @Lazy
    @Relationship(type = "manages", direction = Relationship.Direction.OUTGOING)
    private List<SolarSystem> relationManages;

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                '}';
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getUsername() {
        return name;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void addMySystems(SolarSystem mySystems) {
        this.relationOwns.add(mySystems);
    }
}