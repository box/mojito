package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Locates the line that declares each text unit in an asset, for the formats that declare a text
 * unit with a name attribute (for example Android strings: {@code <string name="merge_board">}).
 *
 * <p>Formats like PO or Mac strings reference the source code a text unit is used in, but formats
 * like Android strings don't, which leaves the PR review comments with no location to report. The
 * declaration line is the location that matters for a pull request anyway: it is the line that was
 * just added or modified.
 */
@Component
public class DeclarationLineLocator {

  static final String GROUP_NAME_FOR_TEXT_UNIT_NAME_IN_SOURCE = "s";

  /**
   * Matches either an XML comment, which is skipped, or the name attribute of a declaration, for
   * example {@code name="merge_board"}
   */
  static final Pattern COMMENT_OR_NAME_ATTRIBUTE_PATTERN =
      Pattern.compile("<!--.*?-->|\\bname\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.DOTALL);

  /**
   * @return the line number of each name declared in the asset content, skipping the XML comments
   */
  private Map<String, Integer> getLineNumbersByDeclaredName(String assetContent) {

    Map<String, Integer> lineNumbersByDeclaredName = new HashMap<>();

    Matcher matcher = COMMENT_OR_NAME_ATTRIBUTE_PATTERN.matcher(assetContent);
    int lineNumber = 1;
    int lineScanIndex = 0;

    while (matcher.find()) {

      String declaredName = matcher.group(1);

      if (declaredName == null) {
        continue;
      }

      while (lineScanIndex < matcher.start(1)) {
        if (assetContent.charAt(lineScanIndex) == '\n') {
          lineNumber++;
        }
        lineScanIndex++;
      }

      lineNumbersByDeclaredName.putIfAbsent(declaredName, lineNumber);
    }

    return lineNumbersByDeclaredName;
  }

  /**
   * @return the name of the text unit as declared in the asset, according to the patterns of the
   *     file type, or the text unit name when there is no pattern or it doesn't match
   */
  String getTextUnitNameInSource(String textUnitName, FileType fileType, boolean isPluralForm) {

    Pattern pattern =
        fileType == null
            ? null
            : isPluralForm
                ? fileType.getTextUnitNameToTextUnitNameInSourcePlural()
                : fileType.getTextUnitNameToTextUnitNameInSourceSingular();

    if (pattern != null) {
      Matcher matcher = pattern.matcher(textUnitName);
      if (matcher.matches()) {
        return matcher.group(GROUP_NAME_FOR_TEXT_UNIT_NAME_IN_SOURCE);
      }
    }

    return textUnitName;
  }

  /**
   * Gets the line number of the declaration of a text unit.
   *
   * <p>A plural form is looked up by the name of its plural group first, since all the forms are
   * declared in that single block. Other text units are looked up by their name first, then by
   * their name in source, so that an array item is found with the name of its array.
   *
   * @return the line number or <code>null</code> if the text unit name is not declared
   */
  Integer getLineNumber(
      AssetExtractorTextUnit assetExtractorTextUnit,
      FileType fileType,
      Map<String, Integer> lineNumbersByDeclaredName) {

    String name = assetExtractorTextUnit.getName();

    if (name == null || name.isEmpty()) {
      return null;
    }

    boolean isPluralForm = assetExtractorTextUnit.getPluralForm() != null;
    String nameInSource = this.getTextUnitNameInSource(name, fileType, isPluralForm);

    List<String> namesToLookUp =
        isPluralForm ? List.of(nameInSource, name) : List.of(name, nameInSource);

    return namesToLookUp.stream()
        .map(lineNumbersByDeclaredName::get)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  /**
   * Sets the declaration location of the text units whose name is declared in the asset.
   *
   * <p>The name declarations are indexed in a single pass over the asset content and the first
   * declaration of a name wins. The forms of a plural group and the items of an array share the
   * line of the block that declares them.
   *
   * @param assetExtractorTextUnits the text units extracted from the asset
   * @param assetContent the content of the asset the text units were extracted from
   * @param assetPath the path of the asset, used as the file of the declaration location
   * @param fileType the type of the asset, which maps a text unit name to its name in the asset
   */
  public void setDeclarationLocations(
      List<AssetExtractorTextUnit> assetExtractorTextUnits,
      String assetContent,
      String assetPath,
      FileType fileType) {

    Map<String, Integer> lineNumbersByDeclaredName =
        this.getLineNumbersByDeclaredName(assetContent);

    if (lineNumbersByDeclaredName.isEmpty()) {
      return;
    }

    for (AssetExtractorTextUnit assetExtractorTextUnit : assetExtractorTextUnits) {
      Integer lineNumber =
          this.getLineNumber(assetExtractorTextUnit, fileType, lineNumbersByDeclaredName);

      if (lineNumber != null) {
        assetExtractorTextUnit.setDeclarationLocation(assetPath + ":" + lineNumber);
      }
    }
  }
}
