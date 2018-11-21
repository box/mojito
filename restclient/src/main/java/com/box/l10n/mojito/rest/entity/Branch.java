package com.box.l10n.mojito.rest.entity;

/**
 * Entity that describes a branch.
 * This entity mirrors: com.box.l10n.mojito.entity.Branch
 *
 * @author jeanaurambault
 */
public class Branch {

    private Long id;

    private String name;

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
}
