package com.box.l10n.mojito;

import com.google.common.base.Splitter;
import com.google.common.collect.HashBiMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LocaleMappingHelper {

  /**
   * Gets the locale mapping given the locale mapping param.
   *
   * @param localeMapppingParam locale mapping param coming from the CLI
   * @return A map containing the locale mapping (key: output tag, value: the tag in the repository)
   */
  public Map<String, String> getLocaleMapping(String localeMapppingParam) {

    Map<String, String> localeMappings = null;
    if (localeMapppingParam != null) {
      localeMappings = Splitter.on(",").withKeyValueSeparator(":").split(localeMapppingParam);
    }

    return localeMappings;
  }

  /**
   * Gets the inverse locale mapping given the locale mapping param
   *
   * @param localeMapppingParam locale mapping param coming from the CLI
   * @return A map containing the inverse locale mapping (key: the tag in the repository, value:
   *     file output tag)
   */
  public Map<String, String> getInverseLocaleMapping(String localeMapppingParam) {

    Map<String, String> inverseLocaleMapping = null;

    if (localeMapppingParam != null) {
      inverseLocaleMapping = HashBiMap.create(getLocaleMapping(localeMapppingParam)).inverse();
    }

    return inverseLocaleMapping;
  }
}
