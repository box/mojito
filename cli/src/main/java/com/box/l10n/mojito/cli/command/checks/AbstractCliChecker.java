package com.box.l10n.mojito.cli.command.checks;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class AbstractCliChecker {

  protected CliCheckerOptions cliCheckerOptions;

  public static final String BULLET_POINT = "\t• ";

  public static AbstractCliChecker createInstanceForClassName(String className)
      throws CliCheckerInstantiationException {
    try {
      Class<?> clazz = Class.forName(className);
      return (AbstractCliChecker) clazz.newInstance();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new CliCheckerInstantiationException(
          "Cannot create an instance of CliChecker using reflection", e);
    }
  }

  public abstract CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs);

  public boolean isHardFail() {
    return cliCheckerOptions.getHardFailureSet().contains(getCliCheckerType());
  }

  public void setCliCheckerOptions(CliCheckerOptions options) {
    this.cliCheckerOptions = options;
  }

  /**
   * Gets added text units from the diff but applies extra filter to handle PO file and extractor
   * edge cases.
   *
   * <p>In the case of gettext extractor and PO files there is an edge case that can lead to check
   * existing strings eventhough they are not changed directly. If the string does not pass the
   * check it will raise a failure which becomes pretty confusing. No error should be raised when
   * removing old strings, or moving them around(include changing files, etc).
   *
   * <p>Code base contains following gettext calls:
   *
   * <pre>
   * # comment1
   * _(source1, context1)
   *
   * # comment2
   * _(source1, context1)
   *
   * # comment3
   * _(source1, context1)
   * </pre>
   *
   * <p>This will get extracted as a single text unit in a PO file like this
   *
   * <pre>
   * #. comment1
   * #. comment2
   * #. comment3
   * #: usage1
   * #: usage2
   * #: usage3
   * msgctxt "context1"
   * msgid "source1"
   * msgstr ""
   * </pre>
   *
   * <p>And after filtering in Mojito, the comments get joined with returned lines {source1,
   * context1, "comment1\ncomment2\ncomment3"}
   *
   * <p>Following code is removed from the code base:
   *
   * <pre>
   * # comment2
   * _(source1, context1)
   * </pre>
   *
   * <p>The PO extractor will now produce:
   *
   * <pre>
   * #. comment1
   * #. comment3
   * #: usage1
   * #: usage3
   * msgctxt "context1"
   * msgid "source1"
   * msgstr ""
   * </pre>
   *
   * <p>And after filtering in Mojito, the comments get joined with returned lines {source1,
   * comment1, "comment1\ncomment3"}
   *
   * <p>The asset extraction diff will contain: added={source1, context1, "comment1\ncomment3"}
   * removed={source1, context1, "comment1\ncomment2\ncomment3"}
   *
   * <p>In that case we don't want to run checks on the added text unit because if the text unit is
   * not compliant it will fail and look odd as the developer is just removing old code (also covers
   * reodering of the comment by the extractor for xyz reasons). On the other hand we don't want to
   * miss running the check when string were reused somewhere else with yet another comment.
   *
   * <p>This checks for any added text unit if there is a matching text unit in "removed" based on
   * the source and content only. Then check that the "added" comments are a subset of the "removed"
   * comments. The set is built on the assemption that we can split by '\n'
   *
   * @param assetExtractionDiffs
   * @return
   */
  protected List<AssetExtractorTextUnit> getAddedTextUnitsExcludingInconsistentComments(
      List<AssetExtractionDiff> assetExtractionDiffs) {
    return assetExtractionDiffs.stream()
        .map(
            assetExtractionDiff -> {
              Map<String, AssetExtractorTextUnit> mapNameAndContentToRemovedTextUnit =
                  assetExtractionDiff.getRemovedTextunits().stream()
                      .collect(
                          toMap(
                              assetExtractorTextUnit ->
                                  assetExtractorTextUnit.getName()
                                      + assetExtractorTextUnit.getSource(),
                              identity()));

              return assetExtractionDiff.getAddedTextunits().stream()
                  .filter(
                      addedTextUnit -> {
                        boolean shouldIncludeTextUnit = true;

                        AssetExtractorTextUnit removed =
                            mapNameAndContentToRemovedTextUnit.get(
                                addedTextUnit.getName() + addedTextUnit.getSource());
                        if (removed != null) {
                          Set<String> commentsInRemoved = getCommentsAsSet(removed);
                          Set<String> commentsInAdded = getCommentsAsSet(addedTextUnit);
                          SetView<String> inAddedButNotInRemoved =
                              Sets.difference(commentsInAdded, commentsInRemoved);
                          if (inAddedButNotInRemoved.isEmpty()) {
                            // there is no new comments introduced (ie. one or many have been
                            // removed, or order may have changed). We don't need to review the text
                            // unit
                            shouldIncludeTextUnit = false;
                          }
                        }

                        return shouldIncludeTextUnit;
                      })
                  .collect(toList());
            })
        .flatMap(List::stream)
        .collect(toList());
  }

  Set<String> getCommentsAsSet(AssetExtractorTextUnit removedTextUnit) {
    return Arrays.stream(removedTextUnit.getComments().split("\n")).collect(Collectors.toSet());
  }

  protected CliCheckResult createCliCheckerResult() {
    return new CliCheckResult(isHardFail(), getCliCheckerType().name());
  }

  protected CliCheckerType getCliCheckerType() {
    return CliCheckerType.findByClass(this.getClass());
  }

  protected List<String> getSourceStringsFromDiff(List<AssetExtractionDiff> assetExtractionDiffs) {
    return getAddedTextUnitsExcludingInconsistentComments(assetExtractionDiffs).stream()
        .map(AssetExtractorTextUnit::getSource)
        .collect(toList());
  }

  protected List<Pattern> getRegexPatterns() {
    return cliCheckerOptions.getParameterRegexSet().stream()
        .map(regex -> Pattern.compile(regex.getRegex()))
        .collect(toList());
  }
}
