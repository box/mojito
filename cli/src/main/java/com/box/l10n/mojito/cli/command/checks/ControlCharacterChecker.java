package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.google.common.base.CharMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ControlCharacterChecker extends AbstractCliChecker {

  class ControlCharacterCheckerResult {
    boolean isSuccessful = true;
    String failureText = "";
  }

  @Override
  public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {
    List<String> failures =
        getSourceStringsFromDiff(assetExtractionDiffs).stream()
            .map(this::getControlCharacterCheckerResult)
            .filter(result -> !result.isSuccessful)
            .map(result -> result.failureText)
            .collect(Collectors.toList());
    CliCheckResult result = createCliCheckerResult();
    if (!failures.isEmpty()) {
      result.setSuccessful(false);
      result.setNotificationText(getNotificationText(failures));
    }
    return result;
  }

  private ControlCharacterCheckerResult getControlCharacterCheckerResult(String source) {
    ControlCharacterCheckerResult result = new ControlCharacterCheckerResult();
    char[] characters = source.toCharArray();
    List<Integer> indexMatches = new ArrayList<>();
    for (int i = 0; i < characters.length; i++) {
      if (CharMatcher.anyOf("\t\r\n")
          .negate()
          .and(CharMatcher.javaIsoControl())
          .matches(characters[i])) {
        indexMatches.add(i);
        result.isSuccessful = false;
      }
    }

    if (!result.isSuccessful) {
      StringBuilder sb = new StringBuilder();
      sb.append(BULLET_POINT)
          .append("Control character found in source string ")
          .append(QUOTE_MARKER)
          .append(source)
          .append(QUOTE_MARKER)
          .append(" at index ")
          .append(
              indexMatches.stream()
                  .map(index -> Integer.toString(index))
                  .collect(Collectors.joining(", ")));
      sb.append(".");
      result.failureText = sb.toString();
    }
    return result;
  }

  private String getNotificationText(List<String> failures) {
    StringBuilder sb = new StringBuilder();
    sb.append(failures.stream().collect(Collectors.joining(System.lineSeparator()))).toString();
    sb.append(System.lineSeparator() + System.lineSeparator());
    sb.append("Please remove control characters from source strings.");
    return sb.toString();
  }
}
