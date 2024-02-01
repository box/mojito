package com.box.l10n.mojito.service.rollback;

import java.util.ArrayList;
import java.util.List;

/**
 * @author aloison
 */
public class CurrentVariantRollbackParameters {

  /** List of {@link com.box.l10n.mojito.entity.Locale#id}s */
  List<Long> localeIds = new ArrayList<>();

  /** List of {@link com.box.l10n.mojito.entity.TMTextUnit#id}s */
  List<Long> tmTextUnitIds = new ArrayList<>();

  public List<Long> getLocaleIds() {
    return localeIds;
  }

  public void setLocaleIds(List<Long> localeIds) {
    this.localeIds = localeIds;
  }

  public List<Long> getTmTextUnitIds() {
    return tmTextUnitIds;
  }

  public void setTmTextUnitIds(List<Long> tmTextUnitIds) {
    this.tmTextUnitIds = tmTextUnitIds;
  }
}
