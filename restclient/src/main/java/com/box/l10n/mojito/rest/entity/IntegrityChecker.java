package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.Comparator;

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

    public static Comparator<IntegrityChecker> getComparator() {
        return new Comparator<IntegrityChecker>() {
            @Override
            public int compare(IntegrityChecker integrityChecker1, IntegrityChecker integrityChecker2) {
                String extension1 = integrityChecker1.getAssetExtension();
                String extension2 = integrityChecker2.getAssetExtension();
                return extension1.compareTo(extension2);
            }
        };
    }
}
