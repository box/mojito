package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.rest.leveraging.CopyTmConfig;
import com.box.l10n.mojito.rest.leveraging.CopyTmConfig.OverwriteMode;
import com.box.l10n.mojito.rest.leveraging.CopyTmConfig.PreserveStatusMode;
import com.box.l10n.mojito.service.tm.AddTMTextUnitCurrentVariantResult;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantCommentService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles leveraging for the copyTm (target leveraging) flow.
 *
 * <p>For each target TMTextUnit, the leveraging pipeline:
 *
 * <ol>
 *   <li>Finds candidates for leveraging with the best possible precision (MD5 > name+content >
 *       name-only/content-only)
 *   <li>Determines effective status based on match precision and candidate TU uniqueness
 *       (overridden by preserve status mode)
 *   <li>Selects best available translation candidate per locale (tiebreaking by TU in-use status
 *       and each translation's recency)
 *   <li>Filters selected translation candidates comparing their statuses against existing
 *       translation (overridden by overwrite mode)
 *   <li>Writes the leveraged translations to DB
 * </ol>
 *
 * @author wwawrzenczak
 */
@Component
public class CopyTmLeverager {

  static Logger logger = LoggerFactory.getLogger(CopyTmLeverager.class);

  @Autowired private TextUnitSearcher textUnitSearcher;

  @Autowired private TMService tmService;

  @Autowired private TMTextUnitVariantCommentService tmTextUnitVariantCommentService;

  @Autowired private TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  public void performLeveragingFor(
      List<TMTextUnit> tmTextUnits,
      Long sourceTmId,
      Long sourceAssetId,
      CopyTmConfig.Mode mode,
      PreserveStatusMode preserveStatusMode,
      OverwriteMode overwriteMode) {

    logger.debug(
        "Perform copy TM leveraging for {} text units, mode: {}, preserveStatus: {}, overwrite: {}",
        tmTextUnits.size(),
        mode,
        preserveStatusMode,
        overwriteMode);

    for (Iterator<TMTextUnit> it = tmTextUnits.iterator(); it.hasNext(); ) {
      TMTextUnit tmTextUnit = it.next();

      boolean didLeverage =
          leverageUnit(
              tmTextUnit, sourceTmId, sourceAssetId, mode, preserveStatusMode, overwriteMode);

      if (didLeverage) {
        it.remove();
      }
    }
  }

  @Transactional
  private boolean leverageUnit(
      TMTextUnit tmTextUnit,
      Long sourceTmId,
      Long sourceAssetId,
      CopyTmConfig.Mode mode,
      PreserveStatusMode preserveStatusMode,
      OverwriteMode overwriteMode) {

    logger.debug(
        "Looking for leveraging candidates for TMTextUnit id: {}, name: {}",
        tmTextUnit.getId(),
        tmTextUnit.getName());

    // Find candidates
    MatchedCandidates matched = findBestMatchLevel(tmTextUnit, sourceTmId, sourceAssetId, mode);

    if (matched.candidates.isEmpty()) {
      logger.debug(
          "No candidates found for TMTextUnit id: {}, name: {}",
          tmTextUnit.getId(),
          tmTextUnit.getName());
      return false;
    }

    // Count distinct TUs and distinct sources (used to determine effective status)
    // note: this is intentionally counted across locales
    // - we don't want to accidentally preserve status of mismatched translations
    // even if they are effectively unambiguous within their locale
    // Example:
    // A: one.properties   application.greeting  "Hello"       DE "Hallo" FR null
    // B: other.properties application.greeting  "Hello World" DE null    FR "Bonjour le monde"
    long distinctTusCount =
        matched.candidates.stream().map(TextUnitDTO::getTmTextUnitId).distinct().count();
    long distinctSourcesCount =
        matched.candidates.stream().map(TextUnitDTO::getSource).distinct().count();

    List<LeveragingDecision> decisions = new ArrayList<>();

    Map<Long, List<TextUnitDTO>> byLocale =
        matched.candidates.stream().collect(Collectors.groupingBy(TextUnitDTO::getLocaleId));
    for (var matchesForLocale : byLocale.values()) {
      SelectedTiebreaker selected = selectByTiebreaker(matchesForLocale);
      // Count distinct candidates for current locale translations
      long distinctTranslationsCount =
          matchesForLocale.stream().map(TextUnitDTO::getTarget).distinct().count();

      TextUnitDTO translation = selected.translation;

      EffectiveStatus effectiveStatus =
          computeEffectiveStatus(
              translation.getStatus(),
              preserveStatusMode,
              matched.level,
              distinctTusCount,
              distinctSourcesCount,
              distinctTranslationsCount);

      String comment =
          buildLeverageComment(translation, matched.level, selected.reason, effectiveStatus.reason);

      decisions.add(new LeveragingDecision(translation, effectiveStatus.status, comment));
    }

    // Filter by overwrite mode
    if (overwriteMode != OverwriteMode.ALL) {
      // fetch statuses for each existing translation of the TU we're about to overwrite
      Map<Long, TMTextUnitVariant.Status> currentStatuses =
          tmTextUnitCurrentVariantRepository.findByTmTextUnit_Id(tmTextUnit.getId()).stream()
              .collect(
                  Collectors.toMap(
                      cv -> cv.getLocale().getId(),
                      cv -> cv.getTmTextUnitVariant().getStatus(),
                      (one, other) -> one.isHigherOrEqualTo(other) ? one : other));

      decisions.removeIf(
          decision -> {
            TMTextUnitVariant.Status current =
                currentStatuses.get(decision.translation.getLocaleId());
            if (current == null) {
              return false;
            }
            TMTextUnitVariant.Status candidateStatus = decision.translation.getStatus();
            boolean blocked =
                switch (overwriteMode) {
                  case NONE -> true;
                  case FOR_TRANSLATION -> current != TMTextUnitVariant.Status.TRANSLATION_NEEDED;
                  case HIGHER_STATUS -> !candidateStatus.isHigherThan(current);
                  case HIGHER_OR_EQUAL_STATUS -> !candidateStatus.isHigherOrEqualTo(current);
                  case ALL -> false;
                };
            if (blocked) {
              logger.debug(
                  "Skipping locale {} for tmTextUnit {} due to overwrite mode {}",
                  decision.translation.getLocaleId(),
                  tmTextUnit.getId(),
                  overwriteMode);
            }
            return blocked;
          });
    }

    for (LeveragingDecision decision : decisions) {
      writeLeveragedTranslation(tmTextUnit, decision);
    }

    return true;
  }

  /**
   * Tries match levels in descending precision order and returns the first level that yields
   * results. This avoids issuing lower-precision queries when a higher-precision match exists.
   *
   * <p>MD5 matching uses the DB's MD5 field so the classification stays correct if the MD5
   * computation changes. Lower levels are classified by comparing individual fields in memory.
   */
  MatchedCandidates findBestMatchLevel(
      TMTextUnit tmTextUnit, Long sourceTmId, Long sourceAssetId, CopyTmConfig.Mode mode) {

    List<TextUnitDTO> md5Matches = searchByMd5(tmTextUnit, sourceTmId, sourceAssetId);
    if (!md5Matches.isEmpty()) {
      return new MatchedCandidates(md5Matches, MatchLevel.MD5);
    }

    if (mode == CopyTmConfig.Mode.MD5) {
      return new MatchedCandidates(List.of(), MatchLevel.MD5);
    }

    List<TextUnitDTO> modeMatches;
    MatchLevel level;
    switch (mode) {
      case NAME -> {
        modeMatches = searchByName(tmTextUnit, sourceTmId, sourceAssetId);
        level = MatchLevel.NAME_ONLY;
      }
      case EXACT -> {
        modeMatches = searchByContent(tmTextUnit, sourceTmId, sourceAssetId);
        level = MatchLevel.CONTENT_ONLY;
      }
      default -> throw new UnsupportedOperationException("Unexpected match mode: " + mode);
    }

    List<TextUnitDTO> nameAndContentMatches =
        modeMatches.stream()
            .filter(
                match ->
                    Objects.equals(match.getName(), tmTextUnit.getName())
                        && Objects.equals(match.getSource(), tmTextUnit.getContent()))
            .toList();

    if (!nameAndContentMatches.isEmpty()) {
      return new MatchedCandidates(nameAndContentMatches, MatchLevel.NAME_AND_CONTENT);
    }

    return new MatchedCandidates(modeMatches, level);
  }

  private List<TextUnitDTO> searchByMd5(
      TMTextUnit tmTextUnit, Long sourceTmId, Long sourceAssetId) {
    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setTmId(sourceTmId);
    params.setAssetId(sourceAssetId);
    params.setMd5(tmTextUnit.getMd5());
    params.setStatusFilter(StatusFilter.TRANSLATED);
    return textUnitSearcher.search(params);
  }

  private List<TextUnitDTO> searchByName(
      TMTextUnit tmTextUnit, Long sourceTmId, Long sourceAssetId) {
    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setTmId(sourceTmId);
    params.setAssetId(sourceAssetId);
    params.setName(tmTextUnit.getName());
    params.setStatusFilter(StatusFilter.TRANSLATED);
    return textUnitSearcher.search(params);
  }

  private List<TextUnitDTO> searchByContent(
      TMTextUnit tmTextUnit, Long sourceTmId, Long sourceAssetId) {
    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setTmId(sourceTmId);
    params.setAssetId(sourceAssetId);
    params.setSource(tmTextUnit.getContent());
    params.setStatusFilter(StatusFilter.TRANSLATED);
    if (tmTextUnit.getPluralForm() != null) {
      params.setPluralFormId(tmTextUnit.getPluralForm().getId());
    } else {
      params.setPluralFormsExcluded(true);
    }
    return textUnitSearcher.search(params);
  }

  /**
   * From a list of candidate translations for a single locale, picks the best one: used over
   * unused, then higher status, then most recently translated.
   */
  private SelectedTiebreaker selectByTiebreaker(List<TextUnitDTO> localeCandidates) {
    if (localeCandidates.size() == 1) {
      return new SelectedTiebreaker(localeCandidates.get(0), "unique");
    }

    List<TextUnitDTO> sorted =
        localeCandidates.stream()
            .sorted(
                Comparator.comparing((TextUnitDTO dto) -> !dto.isUsed())
                    .thenComparing(dto -> -dto.getStatus().ordinal())
                    .thenComparing(
                        TextUnitDTO::getCreatedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

    TextUnitDTO best = sorted.get(0);
    TextUnitDTO runnerUp = sorted.get(1);

    String tiebreaker;
    if (best.isUsed() != runnerUp.isUsed()) {
      tiebreaker = "used over unused";
    } else if (best.getStatus() != runnerUp.getStatus()) {
      tiebreaker = "higher status";
    } else {
      tiebreaker = "most recently translated";
    }

    return new SelectedTiebreaker(best, tiebreaker);
  }

  /**
   * Computes the effective status for a leveraged translation. Returns the candidate's original
   * status if the match is trustworthy, or TRANSLATION_NEEDED if it should be flagged for review.
   *
   * <p>When multiple TUs match, the "candidates agree" checks can prevent unnecessary downgrades:
   *
   * <ul>
   *   <li>If all candidate TUs have the same source content AND translations for this locale agree
   *       (or only one candidate has a translation), the match is effectively unambiguous.
   *   <li>If candidate TUs have different source, or translations disagree, the match is ambiguous
   *       and gets downgraded.
   * </ul>
   */
  private EffectiveStatus computeEffectiveStatus(
      TMTextUnitVariant.Status candidateStatus,
      PreserveStatusMode preserveStatusMode,
      MatchLevel matchLevel,
      long distinctTusCount,
      long distinctSourcesCount,
      long distinctTranslationsCount) {

    boolean uniqueMatch = distinctTusCount == 1;
    boolean effectivelyUnambiguous = distinctSourcesCount == 1 && distinctTranslationsCount == 1;

    return switch (preserveStatusMode) {
      case ALL -> new EffectiveStatus(candidateStatus, "preserved (forced)");
      case UNIQUE -> {
        if (uniqueMatch) {
          yield new EffectiveStatus(candidateStatus, "preserved (unique)");
        }
        if (effectivelyUnambiguous) {
          yield new EffectiveStatus(candidateStatus, "preserved (effectively unambiguous)");
        }
        yield new EffectiveStatus(
            TMTextUnitVariant.Status.TRANSLATION_NEEDED, "downgraded (ambiguous)");
      }
      case PRECISION -> {
        if (matchLevel.isHighPrecision()) {
          if (uniqueMatch) {
            yield new EffectiveStatus(candidateStatus, "preserved (high precision, unique)");
          }
          yield new EffectiveStatus(
              TMTextUnitVariant.Status.TRANSLATION_NEEDED,
              "downgraded (high precision, ambiguous)");
        }
        yield new EffectiveStatus(
            TMTextUnitVariant.Status.TRANSLATION_NEEDED, "downgraded (low precision)");
      }
    };
  }

  private String buildLeverageComment(
      TextUnitDTO translation,
      MatchLevel matchLevel,
      String tiebreakerReason,
      String statusReason) {

    return "Leverage by "
        + matchLevel
        + " - from tmTextUnitId: "
        + translation.getTmTextUnitId()
        + ", tmTextUnitVariantId: "
        + translation.getTmTextUnitVariantId()
        + ", tiebreaker: "
        + tiebreakerReason
        + ", status: "
        + statusReason;
  }

  private void writeLeveragedTranslation(TMTextUnit tmTextUnit, LeveragingDecision decision) {
    TextUnitDTO translation = decision.translation;

    AddTMTextUnitCurrentVariantResult result =
        tmService.addTMTextUnitCurrentVariantWithResult(
            tmTextUnit.getId(),
            translation.getLocaleId(),
            translation.getTarget(),
            translation.getTargetComment(),
            decision.effectiveStatus,
            translation.isIncludedInLocalizedFile(),
            null);

    TMTextUnitCurrentVariant currentVariant = result.getTmTextUnitCurrentVariant();

    if (result.isTmTextUnitCurrentVariantUpdated()) {
      tmTextUnitVariantCommentService.copyComments(
          translation.getTmTextUnitVariantId(), currentVariant.getTmTextUnitVariant().getId());

      tmTextUnitVariantCommentService.addComment(
          currentVariant.getTmTextUnitVariant(),
          TMTextUnitVariantComment.Type.LEVERAGING,
          TMTextUnitVariantComment.Severity.INFO,
          decision.comment);
    }
  }

  record MatchedCandidates(List<TextUnitDTO> candidates, MatchLevel level) {}

  record SelectedTiebreaker(TextUnitDTO translation, String reason) {}

  record EffectiveStatus(TMTextUnitVariant.Status status, String reason) {}

  record LeveragingDecision(
      TextUnitDTO translation, TMTextUnitVariant.Status effectiveStatus, String comment) {}
}
