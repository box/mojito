package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Splitter;
import java.util.Map;

/** @author jaurambault */
public class AssetMappingConverter implements IStringConverter<Map<String, String>> {

  @Override
  public Map<String, String> convert(String value) {

    Map<String, String> map = null;

    if (value != null) {
      try {
        map = Splitter.on(";").withKeyValueSeparator(":").split(value);
      } catch (IllegalArgumentException iae) {
        throw new ParameterException("Invalid asset mapping [" + value + "]");
      }
    }

    return map;
  }
}
