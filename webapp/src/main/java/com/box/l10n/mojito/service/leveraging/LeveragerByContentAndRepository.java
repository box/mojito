package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import java.util.List;

/**
 * Searches for leveraging matches based on content across specified repositories. Used by
 * MachineTranslationService to find existing translations for machine translation candidates.
 *
 * @author garion
 */
public class LeveragerByContentAndRepository {

  private final List<Long> repositoryIds;
  private final List<String> repositoryNames;
  private final LeveragingUtils leveragingUtils;

  public LeveragerByContentAndRepository(
      List<Long> repositoryIds, List<String> repositoryNames, LeveragingUtils leveragingUtils) {
    this.repositoryIds = repositoryIds;
    this.repositoryNames = repositoryNames;
    this.leveragingUtils = leveragingUtils;
  }

  public List<TextUnitDTO> getLeveragingMatches(
      TMTextUnit tmTextUnit, Long sourceTmId, Long sourceAssetId) {
    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setTmId(sourceTmId);
    params.setRepositoryIds(repositoryIds);
    params.setRepositoryNames(repositoryNames);
    params.setSource(tmTextUnit.getContent());
    params.setStatusFilter(StatusFilter.TRANSLATED);
    if (tmTextUnit.getPluralForm() != null) {
      params.setPluralFormId(tmTextUnit.getPluralForm().getId());
    } else {
      params.setPluralFormsExcluded(true);
    }
    return leveragingUtils.getTextUnitSearcher().search(params);
  }
}
