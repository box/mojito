package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.checks.AbstractCliChecker.BULLET_POINT;
import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.GLOSSARY_FILE_PATH_KEY;
import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GlossaryCaseCheckerTest {

  private GlossaryCaseChecker glossaryCaseChecker;

  private List<AssetExtractionDiff> assetExtractionDiffs;

  @Before
  public void setup() {
    glossaryCaseChecker = new GlossaryCaseChecker();
    glossaryCaseChecker.setCliCheckerOptions(
        new CliCheckerOptions(
            Sets.newHashSet(),
            Sets.newHashSet(),
            ImmutableMap.<String, String>builder()
                .put(
                    GLOSSARY_FILE_PATH_KEY.getKey(),
                    "target/test-classes/com/box/l10n/mojito/cli/glossarychecker/glossary.json")
                .build()));
    List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A source string with Company in it.");
    addedTUs.add(assetExtractorTextUnit);
    assetExtractionDiffs = new ArrayList<>();
    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    assetExtractionDiffs.add(assetExtractionDiff);
  }

  @Test
  public void testSuccess() {
    CliCheckResult result = glossaryCaseChecker.run(assetExtractionDiffs);
    Assert.assertTrue(result.isSuccessful());
  }

  @Test
  public void testFailure() {
    List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A source string company in it.");
    addedTUs.add(assetExtractorTextUnit);
    List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    assetExtractionDiffs.add(assetExtractionDiff);

    CliCheckResult result = glossaryCaseChecker.run(assetExtractionDiffs);
    Assert.assertFalse(result.isSuccessful());
  }

  @Test
  public void testMajorErrorContainsMajorInNotificationString() {
    List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A source string with company in it.");
    addedTUs.add(assetExtractorTextUnit);
    List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    assetExtractionDiffs.add(assetExtractionDiff);

    CliCheckResult result = glossaryCaseChecker.run(assetExtractionDiffs);
    Assert.assertFalse(result.isSuccessful());
    Assert.assertTrue(result.getNotificationText().contains("MAJOR"));
  }

  @Test
  public void testMultipleGlossaryTermsInString() {
    List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A source string with company and ads Manager in it.");
    addedTUs.add(assetExtractorTextUnit);
    List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    assetExtractionDiffs.add(assetExtractionDiff);

    CliCheckResult result = glossaryCaseChecker.run(assetExtractionDiffs);
    Assert.assertFalse(result.isSuccessful());
    Assert.assertEquals(
        "Glossary check failures:"
            + System.lineSeparator()
            + BULLET_POINT
            + "MAJOR: String "
            + QUOTE_MARKER
            + "A source string with company and ads Manager in it."
            + QUOTE_MARKER
            + " contains glossary term 'Company' which must match exactly."
            + System.lineSeparator()
            + BULLET_POINT
            + "WARN: String "
            + QUOTE_MARKER
            + "A source string with company and ads Manager in it."
            + QUOTE_MARKER
            + " contains glossary term 'Ads Manager' but does not exactly match the glossary term."
            + System.lineSeparator()
            + System.lineSeparator(),
        result.getNotificationText());
  }

  @Test
  public void testGlossaryTermInStringWithAdditionalSpaces() {
    List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A source string with ads                Manager in it.");
    addedTUs.add(assetExtractorTextUnit);
    List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    assetExtractionDiffs.add(assetExtractionDiff);

    CliCheckResult result = glossaryCaseChecker.run(assetExtractionDiffs);
    Assert.assertEquals(
        "Glossary check failures:"
            + System.lineSeparator()
            + BULLET_POINT
            + "WARN: String "
            + QUOTE_MARKER
            + "A source string with ads                Manager in it."
            + QUOTE_MARKER
            + " contains glossary term 'Ads Manager' but does not exactly match the glossary term."
            + System.lineSeparator()
            + System.lineSeparator(),
        result.getNotificationText());
  }

  @Test
  public void testStringWithNoGlossaryTerms() {
    List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A source string with no glossary terms in it.");
    addedTUs.add(assetExtractorTextUnit);
    List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    assetExtractionDiffs.add(assetExtractionDiff);

    CliCheckResult result = glossaryCaseChecker.run(assetExtractionDiffs);
    Assert.assertTrue(result.isSuccessful());
  }

  @Test
  public void testMinorFailuresDontCauseOverallResultFail() {
    List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A source string with ads Manager in it.");
    addedTUs.add(assetExtractorTextUnit);
    List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    assetExtractionDiffs.add(assetExtractionDiff);

    CliCheckResult result = glossaryCaseChecker.run(assetExtractionDiffs);
    Assert.assertTrue(result.isSuccessful());
    Assert.assertEquals(
        "Glossary check failures:"
            + System.lineSeparator()
            + BULLET_POINT
            + "WARN: String "
            + QUOTE_MARKER
            + "A source string with ads Manager in it."
            + QUOTE_MARKER
            + " contains glossary term 'Ads Manager' but does not exactly match the glossary term."
            + System.lineSeparator()
            + System.lineSeparator(),
        result.getNotificationText());
  }

  @Test
  public void testHyphenatedGlossaryTermInString() {
    List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A source string with Ads-Manager in it.");
    addedTUs.add(assetExtractorTextUnit);
    List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    assetExtractionDiffs.add(assetExtractionDiff);

    CliCheckResult result = glossaryCaseChecker.run(assetExtractionDiffs);
    Assert.assertTrue(result.isSuccessful());
    Assert.assertEquals(
        "Glossary check failures:"
            + System.lineSeparator()
            + BULLET_POINT
            + "WARN: String "
            + QUOTE_MARKER
            + "A source string with Ads-Manager in it."
            + QUOTE_MARKER
            + " contains glossary term 'Ads Manager' but does not exactly match the glossary term."
            + System.lineSeparator()
            + System.lineSeparator(),
        result.getNotificationText());
  }

  @Test
  public void testSameTermDifferentCaseInGlossary() {
    List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A source string with Event manager in it.");
    addedTUs.add(assetExtractorTextUnit);
    List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    assetExtractionDiffs.add(assetExtractionDiff);

    CliCheckResult result = glossaryCaseChecker.run(assetExtractionDiffs);
    Assert.assertTrue(result.getNotificationText().isEmpty());
  }

  @Test
  public void testSameTermDifferentCaseInGlossaryFailure() {
    List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A source string with event Manager in it.");
    addedTUs.add(assetExtractorTextUnit);
    List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    assetExtractionDiffs.add(assetExtractionDiff);

    CliCheckResult result = glossaryCaseChecker.run(assetExtractionDiffs);
    Assert.assertEquals(
        "Glossary check failures:"
            + System.lineSeparator()
            + BULLET_POINT
            + "WARN: String "
            + QUOTE_MARKER
            + "A source string with event Manager in it."
            + QUOTE_MARKER
            + " contains glossary terms 'Event Manager' or 'Event manager' but does not exactly match one of the terms."
            + System.lineSeparator()
            + System.lineSeparator(),
        result.getNotificationText());
  }
}
