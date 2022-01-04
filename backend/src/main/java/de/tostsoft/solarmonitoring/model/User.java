package de.tostsoft.solarmonitoring.model;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@Node("User")
public class User implements UserDetails {

    @NonNull
    private String name;
    @NonNull
    private String password;
    @Id
    @GeneratedValue
    private Long id;

    @Lazy
    @Relationship(type = "owns", direction = Relationship.Direction.OUTGOING)
    private List<SolarSystem> relationOwns;

    @Lazy
    @Relationship(type = "manages", direction = Relationship.Direction.OUTGOING)
    private List<SolarSystem> relationManages;

    @NonNull
    private long grafanId;

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