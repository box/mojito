package com.box.l10n.mojito.service.tm;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PluralNameParser {

  LoadingCache<String, Pattern> patternCache;

  public PluralNameParser() {
    patternCache =
        CacheBuilder.newBuilder()
            .maximumSize(10)
            .build(CacheLoader.from(separtor -> getPattern(separtor)));
  }

  public String getPrefix(String name, String separator) {
    Pattern pattern = patternCache.getUnchecked(separator);
    return Optional.of(pattern.matcher(name))
        .filter(Matcher::matches)
        .map(m -> m.group(1))
        .orElse(name);
  }

  Pattern getPattern(String separator) {
    return Pattern.compile("(.*)" + separator + "(zero|one|two|few|many|other)");
  }

  public String toPluralName(String prefix, String name, String separator) {
    return String.format("%s%s%s", prefix, separator, name);
  }
}
