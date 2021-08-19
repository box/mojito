package com.box.l10n.mojito.entity;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.NaturalId;

/**
 * Entity that contains plural forms as defined in CLDR: zero, one, two, few,
 * many, other
 *
 * @author jaurambault
 */
@Entity
@Table(
        name = "plural_form",
        indexes = {
            @Index(name = "UK__PLURAL_FORM__NAME", columnList = "name", unique = true)
        }
)
@BatchSize(size = 6)
public class PluralForm extends BaseEntity {

    @Basic(optional = false)
    @Column(name = "name", length = 5)
    @NaturalId
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
