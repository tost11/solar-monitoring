package de.tostsoft.solarmonitoring.model;

import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document
public class User implements UserDetails {

  @Id
  private String id;

  @NotNull
  @Indexed(unique=true)
  private String name;

  private String password;

  @NotNull
  private ZonedDateTime creationDate;

  private Boolean isAdmin;

  private Boolean isDeleted;

  @NotNull
  private int numAllowedSystems;

  @DocumentReference(lazy = true,lookup = "{ 'ownedBy' : ?#{#self._id} }")
  @ReadOnlyProperty
  private List<SolarSystem> owns;

  @DocumentReference(lazy = false, lookup = "{ 'user' : ?#{#self._id} }")
  @ReadOnlyProperty
  private List<Manages> manges;

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
}
