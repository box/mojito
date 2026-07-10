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
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles leveraging for the copyTm (target leveraging) flow. Replaces the previous approach of
 * chaining multiple AbstractLeverager subclasses.
 *
 * <p>For each target TMTextUnit, queries are tried in descending precision order (MD5 first, then
 * name, then content) and the first level that yields results is used. Within that level, the best
 * TMTextUnit is selected by: used over unused, then most recently translated.
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

    logger.debug("Perform copy TM leveraging, mode: {}", mode);

    for (Iterator<TMTextUnit> it = tmTextUnits.iterator(); it.hasNext(); ) {
      TMTextUnit tmTextUnit = it.next();

      // Try match levels in descending precision (MD5 → name+content → name-only / content-only).
      // The first level that yields translated candidates wins.
      Optional<MatchedCandidates> matched =
          findBestMatchLevel(tmTextUnit, sourceTmId, sourceAssetId, mode);

      if (matched.isEmpty()) {
        logger.debug("No candidates found for TMTextUnit name: {}", tmTextUnit.getName());
        continue;
      }

      // Among candidates at the winning level, pick the best one (used > unused > recency).
      SelectionResult selection = selectBest(matched.get());

      // Remove from the list so subsequent leveraging passes (if any) don't reprocess it.
      it.remove();

      // Determine whether translations should be flagged for re-translation based on match
      // confidence and uniqueness. MD5 and name+content matches are high-confidence (no flag);
      // name-only and content-only are low-confidence (flag as TRANSLATION_NEEDED).
      // Non-unique matches (multiple TUs at the same level) are also flagged.
      boolean translationNeeded =
          computeCopyTmTranslationNeeded(
              preserveStatusMode, selection.matchLevel, selection.uniqueMatch);

      addLeveragedTranslations(
          tmTextUnit,
          selection.translations,
          translationNeeded,
          selection.uniqueMatch,
          selection.matchLevel,
          overwriteMode);
    }
  }

  /**
   * Tries match levels in descending precision order and returns the first level that yields
   * results. This avoids issuing lower-precision queries when a higher-precision match exists.
   *
   * <p>MD5 matching uses the DB's MD5 field so the classification stays correct if the MD5
   * computation changes. Lower levels are classified by comparing individual fields in memory.
   */
  Optional<MatchedCandidates> findBestMatchLevel(
      TMTextUnit tmTextUnit, Long sourceTmId, Long sourceAssetId, CopyTmConfig.Mode mode) {

    // Each mode defines which match levels to try, from highest to lowest precision.
    // MD5 mode: only exact MD5 matches.
    // NAME mode: MD5 → name+content (comment changed) → name-only (source text changed).
    // EXACT mode: MD5 → name+content → content-only (key renamed but source text identical).
    List<MatchLevel> levels =
        switch (mode) {
          case MD5 -> List.of(MatchLevel.MD5);
          case NAME -> List.of(MatchLevel.MD5, MatchLevel.NAME_AND_CONTENT, MatchLevel.NAME_ONLY);
          case EXACT ->
              List.of(MatchLevel.MD5, MatchLevel.NAME_AND_CONTENT, MatchLevel.CONTENT_ONLY);
          default -> throw new UnsupportedOperationException("Unsupported mode: " + mode);
        };

    // NAME_AND_CONTENT and NAME_ONLY both come from a name-based search, so we cache the
    // results to avoid querying twice.
    List<TextUnitDTO> byNameResults = null;

    for (MatchLevel level : levels) {
      List<TextUnitDTO> candidates;
      switch (level) {
        case MD5 -> {
          // Uses the DB's tu.md5 field for matching, so it stays correct even if the
          // MD5 computation (which fields are hashed) changes in the future.
          candidates = searchByMd5(tmTextUnit, sourceTmId, sourceAssetId);
        }
        case NAME_AND_CONTENT, NAME_ONLY -> {
          if (byNameResults == null) {
            byNameResults = searchByName(tmTextUnit, sourceTmId, sourceAssetId);
          }
          // Filter the name-search results to only those matching at this specific level.
          candidates =
              byNameResults.stream().filter(dto -> matchesLevel(dto, tmTextUnit, level)).toList();
        }
        case CONTENT_ONLY -> {
          // Separate query needed since content-only matches have different names
          // and wouldn't appear in a name-based search.
          candidates =
              searchByContent(tmTextUnit, sourceTmId, sourceAssetId).stream()
                  .filter(dto -> matchesLevel(dto, tmTextUnit, level))
                  .toList();
        }
        default -> throw new UnsupportedOperationException("Unsupported level: " + level);
      }

      if (!candidates.isEmpty()) {
        return Optional.of(new MatchedCandidates(candidates, level));
      }
    }

    return Optional.empty();
  }

  /**
   * Checks whether a candidate matches the target at exactly the given level — not higher, not
   * lower. For example, NAME_ONLY requires the name to match but the content to differ (otherwise
   * it would be NAME_AND_CONTENT).
   */
  private boolean matchesLevel(TextUnitDTO candidate, TMTextUnit target, MatchLevel level) {
    boolean nameMatches = Objects.equals(candidate.getName(), target.getName());
    boolean contentMatches = Objects.equals(candidate.getSource(), target.getContent());

    return switch (level) {
      case NAME_AND_CONTENT -> nameMatches && contentMatches;
      case NAME_ONLY -> nameMatches && !contentMatches;
      case CONTENT_ONLY -> contentMatches && !nameMatches;
      case MD5 -> true;
    };
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
   * Selects the best TMTextUnit from the candidates at a given match level: used over unused, then
   * most recently translated.
   */
  /**
   * When multiple TMTextUnits match at the same level, picks the best one:
   *
   * <ol>
   *   <li>USED text units (part of a current extraction) over UNUSED ones
   *   <li>Among equal used-status, the one with the most recently created translation variant (not
   *       the TU creation date — a translator may re-translate an older TU)
   * </ol>
   *
   * <p>Also determines uniqueness: if only one TMTextUnit matched at this level, the match is
   * unambiguous and status can be preserved. Multiple matches may indicate the "wrong" TU was
   * picked, so PRECISION mode downgrades the status to TRANSLATION_NEEDED.
   */
  SelectionResult selectBest(MatchedCandidates matched) {
    // Group all locale translations by their source TMTextUnit ID — each group represents
    // one candidate TU with all of its locale translations.
    Map<Long, List<TextUnitDTO>> grouped =
        matched.candidates.stream().collect(Collectors.groupingBy(TextUnitDTO::getTmTextUnitId));

    Comparator<Long> comparator =
        Comparator
            // Used TUs first (isUsed=true → false sorts after true, so negate)
            .<Long, Boolean>comparing(id -> !grouped.get(id).stream().anyMatch(TextUnitDTO::isUsed))
            // Most recently translated first (by variant createdDate, descending)
            .thenComparing(
                id ->
                    grouped.get(id).stream()
                        .map(TextUnitDTO::getCreatedDate)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(ZonedDateTime.now()),
                Comparator.reverseOrder());

    Long bestId = grouped.keySet().stream().min(comparator).orElseThrow();

    return new SelectionResult(grouped.get(bestId), matched.matchLevel, grouped.size() == 1);
  }

  /**
   * Determines whether the leveraged translation should be flagged for re-translation.
   *
   * <p>ALL: never flag. UNIQUE: flag only if the match was ambiguous (multiple TUs at the same
   * level). PRECISION: flag if the match level itself is low-confidence (name-only, content-only)
   * OR if the match was ambiguous.
   */
  boolean computeCopyTmTranslationNeeded(
      PreserveStatusMode preserveStatusMode,
      MatchLevel matchLevel,
      boolean uniqueTMTextUnitMatched) {
    return switch (preserveStatusMode) {
      case ALL -> false;
      case UNIQUE -> !uniqueTMTextUnitMatched;
      case PRECISION -> matchLevel.isTranslationNeeded() || !uniqueTMTextUnitMatched;
    };
  }

  /**
   * Writes leveraged translations into the target TMTextUnit. For each locale, checks the overwrite
   * mode against the target's existing translation status (per-locale). The overwrite decision uses
   * the candidate's original status, not the potentially-downgraded effective status — so
   * HIGHER_STATUS compares against what the source TU actually had.
   */
  @Transactional
  void addLeveragedTranslations(
      TMTextUnit tmTextUnit,
      List<TextUnitDTO> translations,
      boolean translationNeeded,
      boolean uniqueTMTextUnitMatched,
      MatchLevel matchLevel,
      OverwriteMode overwriteMode) {

    logger.debug(
        "Add leveraged translations in tmTextUnit id: {}, matchLevel: {}",
        tmTextUnit.getId(),
        matchLevel);

    Map<Long, TMTextUnitVariant.Status> currentStatusByLocaleId =
        buildCurrentStatusByLocaleId(tmTextUnit, overwriteMode);

    for (TextUnitDTO translation : translations) {
      if (!shouldLeverageLocale(
          currentStatusByLocaleId,
          translation.getLocaleId(),
          translation.getStatus(),
          overwriteMode)) {
        logger.debug(
            "Skipping locale {} for tmTextUnit {} due to status overwrite mode",
            translation.getLocaleId(),
            tmTextUnit.getId());
        continue;
      }

      AddTMTextUnitCurrentVariantResult result =
          tmService.addTMTextUnitCurrentVariantWithResult(
              tmTextUnit.getId(),
              translation.getLocaleId(),
              translation.getTarget(),
              translation.getTargetComment(),
              translationNeeded
                  ? TMTextUnitVariant.Status.TRANSLATION_NEEDED
                  : translation.getStatus(),
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
            getLeverageComment(translation, uniqueTMTextUnitMatched, matchLevel));
      }
    }
  }

  private Map<Long, TMTextUnitVariant.Status> buildCurrentStatusByLocaleId(
      TMTextUnit tmTextUnit, OverwriteMode overwriteMode) {
    if (overwriteMode == OverwriteMode.ALL) {
      return Map.of();
    }
    return tmTextUnitCurrentVariantRepository.findByTmTextUnit_Id(tmTextUnit.getId()).stream()
        .collect(
            Collectors.toMap(
                cv -> cv.getLocale().getId(),
                cv -> cv.getTmTextUnitVariant().getStatus(),
                (s1, s2) -> s1));
  }

  private boolean shouldLeverageLocale(
      Map<Long, TMTextUnitVariant.Status> currentStatusByLocaleId,
      Long localeId,
      TMTextUnitVariant.Status candidateStatus,
      OverwriteMode overwriteMode) {
    TMTextUnitVariant.Status currentStatus = currentStatusByLocaleId.get(localeId);
    return switch (overwriteMode) {
      case NONE -> currentStatus == null;
      case FOR_TRANSLATION ->
          currentStatus == null || currentStatus == TMTextUnitVariant.Status.TRANSLATION_NEEDED;
      case HIGHER_STATUS -> currentStatus == null || candidateStatus.isHigherThan(currentStatus);
      case HIGHER_OR_EQUAL_STATUS ->
          currentStatus == null || candidateStatus.isHigherOrEqualTo(currentStatus);
      case ALL -> true;
    };
  }

  private String getLeverageComment(
      TextUnitDTO translation, boolean uniqueTMTextUnitMatched, MatchLevel matchLevel) {
    return "Copy TM leveraging"
        + " ("
        + matchLevel
        + ")"
        + " - leveraging from tmTextUnitId: "
        + translation.getTmTextUnitId()
        + ", tmTextUnitVariantId: "
        + translation.getTmTextUnitVariantId()
        + ", unique match: "
        + uniqueTMTextUnitMatched;
  }

  record MatchedCandidates(List<TextUnitDTO> candidates, MatchLevel matchLevel) {}

  record SelectionResult(
      List<TextUnitDTO> translations, MatchLevel matchLevel, boolean uniqueMatch) {}
}
