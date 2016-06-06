package com.box.l10n.mojito.rest.entity;

import org.joda.time.DateTime;

/**
 *
 * @author jaurambault
 */
public class Drop {

    Long id;

    String name;

    DateTime lastImportedDate;

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

    public DateTime getLastImportedDate() {
        return lastImportedDate;
    }

    public void setLastImportedDate(DateTime lastImportedDate) {
        this.lastImportedDate = lastImportedDate;
    }

}
