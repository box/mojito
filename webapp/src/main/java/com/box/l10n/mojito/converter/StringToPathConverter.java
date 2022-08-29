package com.box.l10n.mojito.converter;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts a String to a Path.
 *
 * @author jaurambault
 */
@Component
@ConfigurationPropertiesBinding
public class StringToPathConverter implements Converter<String, Path> {

  @Override
  public Path convert(String source) {
    return Paths.get(source);
  }
}
