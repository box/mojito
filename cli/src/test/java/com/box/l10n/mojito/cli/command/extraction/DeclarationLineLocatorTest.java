package com.box.l10n.mojito.cli.command.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.box.l10n.mojito.cli.filefinder.file.AndroidStringsFileType;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.cli.filefinder.file.PropertiesFileType;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeclarationLineLocatorTest {

  static final String STRINGS_XML =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <resources>
          <!-- Title of merge option within Edit board page -->
          <string name="merge_board">Merge board</string>

          <string name="board_reorder_changes_saved">Changes saved!</string>

          <plurals name="people">
              <item quantity="one">%1$d person</item>
              <item quantity="other">%1$d people</item>
          </plurals>
          <string-array name="planets">
              <item>Mercury</item>
              <item>Venus</item>
          </string-array>
          <string name="item_2">Second item</string>
      </resources>
      """;

  static final String ASSET_PATH = "res/values/strings.xml";

  DeclarationLineLocator declarationLineLocator = new DeclarationLineLocator();

  FileType androidStringsFileType = new AndroidStringsFileType();

  private AssetExtractorTextUnit createTextUnit(String name, String pluralForm) {
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setName(name);
    assetExtractorTextUnit.setPluralForm(pluralForm);
    return assetExtractorTextUnit;
  }

  private void setDeclarationLocations(
      String assetContent, AssetExtractorTextUnit... assetExtractorTextUnits) {
    declarationLineLocator.setDeclarationLocations(
        List.of(assetExtractorTextUnits), assetContent, ASSET_PATH, androidStringsFileType);
  }

  @Test
  void setDeclarationLocations() {
    AssetExtractorTextUnit mergeBoard = this.createTextUnit("merge_board", null);
    AssetExtractorTextUnit changesSaved = this.createTextUnit("board_reorder_changes_saved", null);

    this.setDeclarationLocations(STRINGS_XML, mergeBoard, changesSaved);

    assertThat(mergeBoard.getDeclarationLocation()).isEqualTo("res/values/strings.xml:4");
    assertThat(changesSaved.getDeclarationLocation()).isEqualTo("res/values/strings.xml:6");
  }

  @Test
  void setDeclarationLocationsForPluralForms() {
    AssetExtractorTextUnit one = createTextUnit("people_one", "one");
    AssetExtractorTextUnit other = createTextUnit("people_other", "other");

    setDeclarationLocations(STRINGS_XML, one, other);

    assertThat(one.getDeclarationLocation()).isEqualTo("res/values/strings.xml:8");
    assertThat(other.getDeclarationLocation()).isEqualTo("res/values/strings.xml:8");
  }

  @Test
  void setDeclarationLocationsForArrayItems() {
    AssetExtractorTextUnit mercury = createTextUnit("planets_0", null);
    AssetExtractorTextUnit venus = createTextUnit("planets_1", null);

    setDeclarationLocations(STRINGS_XML, mercury, venus);

    assertThat(mercury.getDeclarationLocation()).isEqualTo("res/values/strings.xml:12");
    assertThat(venus.getDeclarationLocation()).isEqualTo("res/values/strings.xml:12");
  }

  @Test
  void setDeclarationLocationsPrefersExactNameOverNameInSource() {
    AssetExtractorTextUnit textUnit = createTextUnit("item_2", null);

    setDeclarationLocations(STRINGS_XML, textUnit);

    assertThat(textUnit.getDeclarationLocation()).isEqualTo("res/values/strings.xml:16");
  }

  @Test
  void setDeclarationLocationsWhenNameIsSingleQuoted() {
    AssetExtractorTextUnit textUnit = createTextUnit("merge_board", null);

    setDeclarationLocations(
        """
        <resources>
            <string name='merge_board'>Merge board</string>
        </resources>
        """,
        textUnit);

    assertThat(textUnit.getDeclarationLocation()).isEqualTo("res/values/strings.xml:2");
  }

  @Test
  void setDeclarationLocationsKeepsFirstDeclaration() {
    AssetExtractorTextUnit textUnit = createTextUnit("merge_board", null);

    setDeclarationLocations(
        """
        <resources>
            <string name="merge_board">Merge board</string>
            <string name="merge_board">Merge board</string>
        </resources>
        """,
        textUnit);

    assertThat(textUnit.getDeclarationLocation()).isEqualTo("res/values/strings.xml:2");
  }

  @Test
  void setDeclarationLocationsSkipsNamesInComments() {
    AssetExtractorTextUnit textUnit = createTextUnit("merge_board", null);

    setDeclarationLocations(
        """
        <resources>
            <!-- Was declared as name="merge_board"
                 before the redesign -->
            <string name="merge_board">Merge board</string>
        </resources>
        """,
        textUnit);

    assertThat(textUnit.getDeclarationLocation()).isEqualTo("res/values/strings.xml:4");
  }

  @Test
  void setDeclarationLocationsWithWindowsLineEndings() {
    AssetExtractorTextUnit textUnit = createTextUnit("merge_board", null);

    setDeclarationLocations(
        "<resources>\r\n    <string name=\"merge_board\">Merge board</string>\r\n</resources>\r\n",
        textUnit);

    assertThat(textUnit.getDeclarationLocation()).isEqualTo("res/values/strings.xml:2");
  }

  @Test
  void setDeclarationLocationsWhenNameIsNotDeclared() {
    AssetExtractorTextUnit textUnit = createTextUnit("key1", null);

    declarationLineLocator.setDeclarationLocations(
        List.of(textUnit), "key1=value1\n", "test.properties", new PropertiesFileType());

    assertThat(textUnit.getDeclarationLocation()).isNull();
  }

  @Test
  void setDeclarationLocationsWhenNoName() {
    AssetExtractorTextUnit textUnit = createTextUnit(null, null);

    setDeclarationLocations(STRINGS_XML, textUnit);

    assertThat(textUnit.getDeclarationLocation()).isNull();
  }

  @Test
  void setDeclarationLocationsLeavesUsagesUnchanged() {
    AssetExtractorTextUnit textUnit = createTextUnit("merge_board", null);

    setDeclarationLocations(STRINGS_XML, textUnit);

    assertThat(textUnit.getUsages()).isNull();
  }

  @Test
  void getTextUnitNameInSource() {
    assertThat(
            declarationLineLocator.getTextUnitNameInSource(
                "people_one", androidStringsFileType, true))
        .isEqualTo("people");
    assertThat(
            declarationLineLocator.getTextUnitNameInSource(
                "planets_0", androidStringsFileType, false))
        .isEqualTo("planets");
    assertThat(
            declarationLineLocator.getTextUnitNameInSource(
                "merge_board", androidStringsFileType, false))
        .isEqualTo("merge_board");
    assertThat(declarationLineLocator.getTextUnitNameInSource("people_one", null, true))
        .isEqualTo("people_one");
  }
}
