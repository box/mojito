package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Entity that assign an integrity check for a type of asset.
 *
 * @author wyau
 */
public class IntegrityChecker {

    private Long id;

    @JsonBackReference
    private Repository repository;

    private String assetExtension;
    private IntegrityCheckerType integrityCheckerType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public String getAssetExtension() {
        return assetExtension;
    }

    public void setAssetExtension(String assetExtension) {
        this.assetExtension = assetExtension;
    }

    public IntegrityCheckerType getIntegrityCheckerType() {
        return integrityCheckerType;
    }

    public void setIntegrityCheckerType(IntegrityCheckerType integrityCheckerType) {
        this.integrityCheckerType = integrityCheckerType;
    }
}
