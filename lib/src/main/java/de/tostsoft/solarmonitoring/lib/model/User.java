package de.tostsoft.solarmonitoring.lib.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Document
public class User implements UserDetails {

  @Id
  protected String id;

  @NotNull
  @Indexed(unique=true)
  protected String name;

  protected String viewName;

  protected String password;

  @NotNull
  protected LocalDateTime creationDate;

  protected Boolean isAdmin;

  @NotNull
  @Indexed(unique=true)
  protected String influxBucketName;

  @NotNull
  protected int numAllowedSystems;

  @DocumentReference(lazy = true,lookup = "{ 'ownedBy' : ?#{#self._id} }")
  @ReadOnlyProperty
  protected List<SolarSystem> owns;

  @DocumentReference(lazy = false, lookup = "{ 'user' : ?#{#self._id} }")
  @ReadOnlyProperty
  protected List<Manages> manges;

  @NotNull
  private LocalDateTime deletedAt;

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

  public boolean equals(User user){
    return StringUtils.equals(id,user.id);
  }

  public List<Manages> getManges() {
    return manges.stream().filter(m->m.getSolarSystem().getDeletedAt() == null).collect(Collectors.toList());
  }

  public List<Manages> seesAllGetManges() {
    return manges;
  }

  public List<SolarSystem> getOwns() {
    return owns.stream().filter(m->m.getDeletedAt() == null).collect(Collectors.toList());
  }

  public List<SolarSystem> seesAllGetOwns() {
    return owns;
  }
}
