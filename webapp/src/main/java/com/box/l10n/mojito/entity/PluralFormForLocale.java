package com.box.l10n.mojito.entity;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Entity that contains plural form per locale.
 *
 * @author jaurambault
 */
@Entity
@Table(
    name = "plural_form_for_locale",
    indexes = {
      @Index(
          name = "UK__PLURAL_FORM__PLURAL_FORM_ID__LOCALE_ID",
          columnList = "plural_form_id, locale_id",
          unique = true)
    })
public class PluralFormForLocale extends BaseEntity {

  @ManyToOne
  @JoinColumn(
      name = "locale_id",
      foreignKey = @ForeignKey(name = "FK__PLURAL_FORM_FOR_LOCALE__LOCALE__ID"),
      nullable = false)
  private Locale locale;

  @ManyToOne
  @JoinColumn(
      name = "plural_form_id",
      foreignKey = @ForeignKey(name = "FK__PLURAL_FORM_FOR_LOCALE__PLURAL_FORM__ID"),
      nullable = false)
  private PluralForm pluralForm;

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public PluralForm getPluralForm() {
    return pluralForm;
  }

  public void setPluralForm(PluralForm pluralForm) {
    this.pluralForm = pluralForm;
  }
}
