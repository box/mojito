package com.box.l10n.mojito.service.pluralform;

import com.box.l10n.mojito.entity.PluralForm;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @author jaurambault
 */
@Service
public class PluralFormService {

  @Autowired PluralFormRepository pluralFormRepository;

  /**
   * @return Map "plural_form" => PluralForm. The map will be cached.
   */
  @Cacheable("pluralForms")
  private Map<String, PluralForm> getPluralFormMap() {
    Map<String, PluralForm> pluralFormsMap = new HashMap<>();
    List<PluralForm> pluralForms = pluralFormRepository.findAll();

    for (PluralForm pluralForm : pluralForms) {
      pluralFormsMap.put(pluralForm.getName(), pluralForm);
    }

    return pluralFormsMap;
  }

  /**
   * @return Map ID => PluralForm. The map will be cached.
   */
  @Cacheable("pluralForms")
  private Map<Long, PluralForm> getPluralFormIdMap() {
    Map<Long, PluralForm> pluralFormsMap = new HashMap<>();
    List<PluralForm> pluralForms = pluralFormRepository.findAll();

    for (PluralForm pluralForm : pluralForms) {
      pluralFormsMap.put(pluralForm.getId(), pluralForm);
    }

    return pluralFormsMap;
  }

  /**
   * Returns the PluralForm for the given plural form string.
   *
   * @param pluralFormString the plural form string
   * @return The corresponding plural form or {@code null} if none found
   */
  public PluralForm findByPluralFormString(String pluralFormString) {

    PluralForm pluralForm = null;

    if (pluralFormString != null) {
      pluralForm = getPluralFormMap().get(pluralFormString.toLowerCase());
    }

    return pluralForm;
  }

  /**
   * Returns the plural form for the given ID.
   *
   * @param pluralFormId The ID of the plural form
   * @return The corresponding plural form or {@code null} if none found
   */
  public PluralForm findById(Long pluralFormId) {
    return getPluralFormIdMap().get(pluralFormId);
  }
}
