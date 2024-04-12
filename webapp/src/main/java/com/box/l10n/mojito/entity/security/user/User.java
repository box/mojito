package com.box.l10n.mojito.entity.security.user;

import com.box.l10n.mojito.entity.AuditableEntity;
import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedBy;

@Entity
@Table(
    name = "user",
    indexes = {@Index(name = "I__USERS__USERNAME", columnList = "username", unique = true)})
@BatchSize(size = 1000)
@NamedEntityGraph(
    name = "User.legacy",
    attributeNodes = {
      @NamedAttributeNode("createdByUser"),
      @NamedAttributeNode(value = "authorities", subgraph = "User.legacy.authorities")
    },
    subgraphs = {
      @NamedSubgraph(
          name = "User.legacy.authorities",
          attributeNodes = {@NamedAttributeNode("createdByUser"), @NamedAttributeNode("user")})
    })
public class User extends AuditableEntity implements Serializable {

  public static final int NAME_MAX_LENGTH = 255;

  @Basic(optional = false)
  @Column(name = "username")
  @JsonView(View.IdAndName.class)
  String username;

  @JsonProperty(access = Access.WRITE_ONLY)
  @Column(name = "password")
  String password;

  @Column(name = "enabled")
  Boolean enabled;

  @Column(name = "surname")
  String surname;

  @Column(name = "given_name")
  String givenName;

  @Column(name = "common_name")
  @JsonView(View.IdAndName.class)
  String commonName;

  /**
   * Sets this flag if the user is created by a process that don't have all the information. Eg.
   * pushing an asset for a branch with an owner or header base authentication. If the owner is not
   * in the system yet, the user will be created but the information will be minimal.
   *
   * <p>Usually user are created the first time the user connect to the system via LDAP or OAuth and
   * have more information Partially created user can be updated later when the first login happens.
   */
  @Column(name = "partially_created")
  Boolean partiallyCreated = false;

  @JsonManagedReference
  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
  Set<Authority> authorities = new HashSet<>();

  @JsonIgnore
  @CreatedBy
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__USER__USER__ID"))
  protected User createdByUser;

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Set<Authority> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(Set<Authority> authorities) {
    this.authorities = authorities;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public String getCommonName() {
    return commonName;
  }

  public void setCommonName(String commonName) {
    this.commonName = commonName;
  }

  public Boolean getPartiallyCreated() {
    return partiallyCreated;
  }

  public void setPartiallyCreated(Boolean partiallyCreated) {
    this.partiallyCreated = partiallyCreated;
  }
}
