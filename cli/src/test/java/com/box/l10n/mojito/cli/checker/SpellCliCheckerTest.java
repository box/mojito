package com.box.l10n.mojito.cli.checker;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.checks.CliCheckerOptions;
import com.box.l10n.mojito.cli.command.checks.CliCheckerType;
import com.box.l10n.mojito.cli.command.checks.SpellCliChecker;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.io.Files;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.PLACEHOLDER_NO_SPECIFIER_REGEX;
import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.PRINTF_LIKE_VARIABLE_TYPE_REGEX;
import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.SINGLE_BRACE_REGEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpellCliCheckerTest {

    private SpellCliChecker spellCliChecker;

    @Before
    public void setup() {
        spellCliChecker = new SpellCliChecker();
        spellCliChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(SINGLE_BRACE_REGEX.getRegex()), Sets.newHashSet(), "", ""));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with no errors.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        spellCliChecker.setAssetExtractionDiffs(assetExtractionDiffs);
    }

    @Test
    public void testHardFailureIsSet() throws Exception {
        Set<String> hardFailureSet = new HashSet<>();
        hardFailureSet.add(CliCheckerType.SPELL_CHECKER.name());
        spellCliChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(PLACEHOLDER_NO_SPECIFIER_REGEX.getRegex()), hardFailureSet, "", ""));
        CliCheckResult result = spellCliChecker.call();
        assertTrue(result.isHardFail());
    }

    @Test
    public void testStringWithNoErrors() throws Exception {
        CliCheckResult result = spellCliChecker.call();
        assertTrue(result.isSuccessful());
        assertTrue(result.getNotificationText().isEmpty());
        assertFalse(result.isHardFail());
    }

    @Test
    public void testStringWithErrors() throws Exception {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source strng with some erors.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        spellCliChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        CliCheckResult result = spellCliChecker.call();
        assertFalse(result.isSuccessful());
        assertFalse(result.getNotificationText().isEmpty());
        assertTrue(result.getNotificationText().contains("\t* 'erors'"));
        assertTrue(result.getNotificationText().contains("\t* 'strng'"));
        assertFalse(result.isHardFail());
    }

    @Test
    public void testAddingStringsToDictionary() throws Exception {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source strng with some erors.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        spellCliChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        createTempDictionaryAdditionsFile("strng" + System.lineSeparator() + "erors");
        spellCliChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(PLACEHOLDER_NO_SPECIFIER_REGEX.getRegex()), Sets.newHashSet(),
                "target/tests/resources/dictAddition.txt", ""));
        CliCheckResult result = spellCliChecker.call();
        assertTrue(result.isSuccessful());
        assertTrue(result.getNotificationText().isEmpty());
        assertFalse(result.isHardFail());
    }

    @Test
    public void testParametersAreNotSpellChecked() throws Exception {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with {numbr} errors.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        spellCliChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        CliCheckResult result = spellCliChecker.call();
        assertTrue(result.isSuccessful());
        assertTrue(result.getNotificationText().isEmpty());
        assertFalse(result.isHardFail());
    }

    @Test
    public void testAddingStringsToDictionaryAppendsSuggestedUpdateOnFailure() throws Exception {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source strng with some erors.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        spellCliChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        createTempDictionaryAdditionsFile("");
        spellCliChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(PLACEHOLDER_NO_SPECIFIER_REGEX.getRegex()), Sets.newHashSet(),
                "target/tests/resources/dictAddition.txt", ""));
        CliCheckResult result = spellCliChecker.call();
        assertFalse(result.isSuccessful());
        assertFalse(result.getNotificationText().isEmpty());
        assertTrue(result.getNotificationText().contains("\t* 'erors'"));
        assertTrue(result.getNotificationText().contains("\t* 'strng'"));
        assertTrue(result.getNotificationText().contains("If the word is correctly spelt please add your spelling to target/tests/resources/dictAddition.txt to avoid future false negatives."));
        assertFalse(result.isHardFail());
    }

    @Test
    public void testMultipleStringsWithMisspellings() throws Exception {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source strng with some erors.");
        addedTUs.add(assetExtractorTextUnit);
        AssetExtractorTextUnit anotherTextUnit = new AssetExtractorTextUnit();
        anotherTextUnit.setSource("Another string with falures");
        addedTUs.add(anotherTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        spellCliChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        CliCheckResult result = spellCliChecker.call();
        assertFalse(result.isSuccessful());
        assertFalse(result.getNotificationText().isEmpty());
        assertTrue(result.getNotificationText().contains("\t* 'erors'"));
        assertTrue(result.getNotificationText().contains("\t* 'strng'"));
        assertTrue(result.getNotificationText().contains("\t* 'falures'"));
    }

    @Test
    public void testNestedICUPlaceholderIsExcludedFromSpellcheck() throws Exception {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("You have {count, plurl, one{1 photo} othr{{count} phutos}}");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        spellCliChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        CliCheckResult result = spellCliChecker.call();
        assertTrue(result.isSuccessful());
        assertTrue(result.getNotificationText().isEmpty());
        assertFalse(result.isHardFail());
    }

    @Test
    public void testMultipleIdenticalErrorsInStringAreReportedOnlyOnce() throws Exception {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with identicl identicl identicl errors.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        spellCliChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        CliCheckResult result = spellCliChecker.call();
        assertFalse(result.isSuccessful());
        assertTrue(result.getNotificationText().contains("\t* 'identicl'"));
        assertEquals(1, result.getNotificationText().chars().filter(c -> c =='*').count());
    }

    @Test
    public void testMultipleConfiguredPlaceholderRegexesAreNotSpellChecked() throws Exception {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with a {numbr} of %(diferent)s errors.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        spellCliChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        spellCliChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(SINGLE_BRACE_REGEX.getRegex(), PRINTF_LIKE_VARIABLE_TYPE_REGEX.getRegex()), Sets.newHashSet(),
                "", ""));
        CliCheckResult result = spellCliChecker.call();
        assertTrue(result.isSuccessful());
        assertTrue(result.getNotificationText().isEmpty());
    }

    private void createTempDictionaryAdditionsFile(String contents) throws IOException {
        File dictAdditions = new File("target/tests/resources/");
        dictAdditions.mkdirs();
        dictAdditions = new File("target/tests/resources/dictAddition.txt");
        dictAdditions.createNewFile();
        Files.write(Paths.get("target/tests/resources/dictAddition.txt"), contents);
    }
}
