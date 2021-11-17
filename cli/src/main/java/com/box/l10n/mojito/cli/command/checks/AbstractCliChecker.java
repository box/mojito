package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class AbstractCliChecker {

    protected CliCheckerOptions cliCheckerOptions;

    public static AbstractCliChecker createInstanceForClassName(String className) throws CliCheckerInstantiationException {
        try {
            Class<?> clazz = Class.forName(className);
            return (AbstractCliChecker) clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new CliCheckerInstantiationException("Cannot create an instance of CliChecker using reflection", e);
        }
    }

    public abstract CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs);

    public boolean isHardFail() {
        return cliCheckerOptions.getHardFailureSet().contains(getCliCheckerType());
    }

    public void setCliCheckerOptions(CliCheckerOptions options) {
        this.cliCheckerOptions = options;
    }

    protected List<AssetExtractorTextUnit> getAddedTextUnits(List<AssetExtractionDiff> assetExtractionDiffs) {
        return assetExtractionDiffs.stream().map(AssetExtractionDiff::getAddedTextunits).flatMap(List::stream).collect(Collectors.toList());
    }

    protected CliCheckResult createCliCheckerResult() {
        return new CliCheckResult(isHardFail(), getCliCheckerType().name());
    }

    protected CliCheckerType getCliCheckerType() {
        return CliCheckerType.findByClass(this.getClass());
    }

    protected List<String> getSourceStringsFromDiff(List<AssetExtractionDiff> assetExtractionDiffs) {
        return getAddedTextUnits(assetExtractionDiffs).stream().map(AssetExtractorTextUnit::getSource).collect(Collectors.toList());
    }

    protected List<Pattern> getRegexPatterns() {
        return cliCheckerOptions.getParameterRegexSet().stream().map(regex -> Pattern.compile(regex.getRegex())).collect(Collectors.toList());
    }

}
