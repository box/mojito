package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity that describes a repository.
 * This entity mirrors: com.box.l10n.mojito.entity.Repository
 *
 * @author wyau
 */
public class Repository {

    private Long id;

    private String name;

    private String description;
    
    private Boolean deleted;

    private Boolean checkSLA;

    private Locale sourceLocale;

    @JsonManagedReference
    Set<RepositoryLocale> repositoryLocales = new HashSet<>();

    @JsonManagedReference
    @JsonProperty("assetIntegrityCheckers")
    Set<IntegrityChecker> integrityCheckers = new HashSet<>();

    RepositoryStatistic repositoryStatistic;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Locale getSourceLocale() {
        return sourceLocale;
    }

    public void setSourceLocale(Locale sourceLocale) {
        this.sourceLocale = sourceLocale;
    }

    public Set<RepositoryLocale> getRepositoryLocales() {
        return repositoryLocales;
    }

    public void setRepositoryLocales(Set<RepositoryLocale> repositoryLocales) {
        this.repositoryLocales = repositoryLocales;
    }

    public Set<IntegrityChecker> getIntegrityCheckers() {
        return integrityCheckers;
    }

    public void setIntegrityCheckers(Set<IntegrityChecker> integrityCheckers) {
        this.integrityCheckers = integrityCheckers;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Boolean getCheckSLA() {
        return checkSLA;
    }

    public void setCheckSLA(Boolean checkSLA) {
        this.checkSLA = checkSLA;
    }

    public RepositoryStatistic getRepositoryStatistic() {
        return repositoryStatistic;
    }

    public void setRepositoryStatistic(RepositoryStatistic repositoryStatistic) {
        this.repositoryStatistic = repositoryStatistic;
    }

}
