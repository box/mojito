package com.box.l10n.mojito.service.thirdparty.smartling;

import static java.lang.Boolean.parseBoolean;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SmartlingOptions {

  public static final String PLURAL_FIX = "smartling-plural-fix";
  public static final String PLACEHOLDER_FORMAT = "smartling-placeholder-format";

  public static final String PLACEHOLDER_FORMAT_CUSTOM = "smartling-placeholder-format-custom";
  public static final String STRING_FORMAT = "smartling-string-format";
  public static final String DRY_RUN = "dry-run";
  public static final String REQUEST_ID = "request-id";
  public static final String JSON_SYNC = "json-sync";
  public static final String GLOSSARY_SYNC = "glossary-sync";
  public static final String PUSH_TRANSALTION_BRANCH_NAME = "push-translation-branch-name";

  private final Set<String> pluralFixForLocales;
  private final String placeholderFormat;
  private final String customPlaceholderFormat;
  private final String stringFormat;
  private final boolean dryRun;
  private final String requestId;
  private final boolean isJsonSync;
  private final boolean isGlossarySync;
  private final String pushTranslationBranchName;

  public SmartlingOptions(
      Set<String> pluralFixForLocales,
      String placeholderFormat,
      String customPlaceholderFormat,
      String stringFormat,
      boolean dryRun,
      String requestId,
      boolean isJsonSync,
      boolean isGlossarySync,
      String pushTranslationBranchName) {
    this.pluralFixForLocales = pluralFixForLocales;
    this.placeholderFormat = placeholderFormat;
    this.customPlaceholderFormat = customPlaceholderFormat;
    this.stringFormat = stringFormat;
    this.dryRun = dryRun;
    this.requestId = requestId;
    this.isJsonSync = isJsonSync;
    this.isGlossarySync = isGlossarySync;
    this.pushTranslationBranchName = pushTranslationBranchName;
  }

  public static SmartlingOptions parseList(List<String> options) {

    Map<String, String> map =
        options.stream()
            .map(str -> str.split("=", 2))
            .filter(arr -> arr.length == 2)
            .collect(Collectors.toMap(a -> a[0], a -> a[1]));

    String pluralFixStr = map.getOrDefault(PLURAL_FIX, "");
    String dryRunStr = map.get(DRY_RUN);
    String placeholderFormat = map.get(PLACEHOLDER_FORMAT);
    String customPlaceholderFormat = map.get(PLACEHOLDER_FORMAT_CUSTOM);
    String stringFormat = map.get(STRING_FORMAT);
    String requestId = map.get(REQUEST_ID);
    String isJsonSync = map.get(JSON_SYNC);
    String isGlossarySync = map.get(GLOSSARY_SYNC);
    String pushTranslationBranchName = map.get(PUSH_TRANSALTION_BRANCH_NAME);

    return new SmartlingOptions(
        pluralFixStr.isEmpty()
            ? Collections.emptySet()
            : ImmutableSet.copyOf(pluralFixStr.split(",")),
        placeholderFormat,
        customPlaceholderFormat,
        stringFormat,
        parseBoolean(dryRunStr),
        requestId,
        parseBoolean(isJsonSync),
        parseBoolean(isGlossarySync),
        pushTranslationBranchName);
  }

  public Set<String> getPluralFixForLocales() {
    return pluralFixForLocales;
  }

  public String getPlaceholderFormat() {
    return placeholderFormat;
  }

  public String getCustomPlaceholderFormat() {
    return customPlaceholderFormat;
  }

  public String getStringFormat() {
    return stringFormat;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public String getRequestId() {
    return requestId;
  }

  public boolean isJsonSync() {
    return isJsonSync;
  }

  public boolean isGlossarySync() {
    return isGlossarySync;
  }

  public String getPushTranslationBranchName() {
    return pushTranslationBranchName;
  }
}
