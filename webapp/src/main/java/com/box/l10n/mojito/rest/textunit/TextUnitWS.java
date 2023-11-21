package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.asset.AssetPathNotFoundException;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckException;
import com.box.l10n.mojito.service.gitblame.GitBlameService;
import com.box.l10n.mojito.service.gitblame.GitBlameWithUsage;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryNameNotFoundException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantService;
import com.box.l10n.mojito.service.tm.TMTextUnitHistoryService;
import com.box.l10n.mojito.service.tm.TMTextUnitIntegrityCheckService;
import com.box.l10n.mojito.service.tm.TMTextUnitStatisticService;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.SearchType;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitAndWordCount;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webservices for the workbench. Allows to search for TextUnits and add/update/delete translations.
 *
 * @author Jean
 */
@RestController
public class TextUnitWS {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(TextUnitWS.class);

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired TMService tmService;

  @Autowired TMTextUnitHistoryService tmHistoryService;

  @Autowired TMTextUnitCurrentVariantService tmTextUnitCurrentVariantService;

  @Autowired TMTextUnitIntegrityCheckService tmTextUnitIntegrityCheckService;

  @Autowired TMTextUnitStatisticService tmTextUnitStatisticService;

  @Autowired AssetTextUnitRepository assetTextUnitRepository;

  @Autowired TextUnitBatchImporterService textUnitBatchImporterService;

  @Autowired GitBlameService gitBlameService;

  @Autowired LocaleService localeService;

  @Autowired AssetRepository assetRepository;

  /**
   * Gets the TextUnits that matches the search parameters.
   *
   * <p>It uses a filter logic. The dataset will be filtered down as more criteria are specified
   * (search for specific locales, string name|target|source, etc).
   *
   * <p>Either repositoryIds, repositoryNames or tmTextUnitIds must be provided in the query
   * parameters
   *
   * @throws InvalidTextUnitSearchParameterException
   */
  @RequestMapping(method = RequestMethod.GET, value = "/api/textunits")
  @ResponseStatus(HttpStatus.OK)
  public List<TextUnitDTO> getTextUnitsWithGet(TextUnitSearchBody textUnitSearchBody)
      throws InvalidTextUnitSearchParameterException {

    return getTextUnits(textUnitSearchBody);
  }

  /**
   * Same as the GET version {@link #getTextUnitsWithGet(TextUnitSearchBody)} but support queries
   * with larger parameters for list of repositories, locales, etc.
   *
   * <p>This entry point is now used by Mojito Frontend. Keeping the GET implementation for other
   * integrations.
   */
  @RequestMapping(method = RequestMethod.POST, value = "/api/textunits/search")
  @ResponseStatus(HttpStatus.OK)
  public List<TextUnitDTO> getTextUnitsWithPost(@RequestBody TextUnitSearchBody textUnitSearchBody)
      throws InvalidTextUnitSearchParameterException {

    return getTextUnits(textUnitSearchBody);
  }

  private List<TextUnitDTO> getTextUnits(TextUnitSearchBody textUnitSearchBody)
      throws InvalidTextUnitSearchParameterException {
    checkMandatoryParamatersForSearch(textUnitSearchBody);

    TextUnitSearcherParameters textUnitSearcherParameters =
        textUnitSearchBodyToTextUnitSearcherParameters(textUnitSearchBody);
    applySharedSearchAndCountParameters(textUnitSearcherParameters);

    List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
    return search;
  }

  void applySharedSearchAndCountParameters(TextUnitSearcherParameters textUnitSearcherParameters) {
    textUnitSearcherParameters.setRootLocaleExcluded(false);
  }

  /**
   * Return the total count of text units that a {@link #getTextUnitsWithGet(TextUnitSearchBody)}
   * search with the same parameters would return.
   */
  @RequestMapping(method = RequestMethod.GET, value = "/api/textunits/count")
  @ResponseStatus(HttpStatus.OK)
  public TextUnitAndWordCount getTextUnitsCount(TextUnitSearchBody textUnitSearchBody)
      throws InvalidTextUnitSearchParameterException {

    checkMandatoryParamatersForSearch(textUnitSearchBody);

    textUnitSearchBody.setLimit(null); // reset default value to count

    TextUnitSearcherParameters textUnitSearcherParameters =
        textUnitSearchBodyToTextUnitSearcherParameters(textUnitSearchBody);
    applySharedSearchAndCountParameters(textUnitSearcherParameters);

    TextUnitAndWordCount countTextUnitAndWordCount =
        textUnitSearcher.countTextUnitAndWordCount(textUnitSearcherParameters);
    return countTextUnitAndWordCount;
  }

  /**
   * Gets the translation history for a given text unit for a particular locale.
   *
   * @param tmTextUnitId required
   * @param bcp47Tag required
   * @return the translations that matches the search parameters
   * @throws InvalidTextUnitSearchParameterException
   */
  @RequestMapping(method = RequestMethod.GET, value = "/api/textunits/{tmTextUnitId}/history")
  @ResponseStatus(HttpStatus.OK)
  @JsonView(View.TranslationHistorySummary.class)
  public List<TMTextUnitVariant> getTextUnitHistory(
      @PathVariable Long tmTextUnitId,
      @RequestParam(value = "bcp47Tag", required = true) String bcp47Tag)
      throws InvalidTextUnitSearchParameterException {

    Locale locale = localeService.findByBcp47Tag(bcp47Tag);
    return tmHistoryService.findHistory(tmTextUnitId, locale.getId());
  }

  void checkMandatoryParamatersForSearch(TextUnitSearchBody textUnitSearchBody)
      throws InvalidTextUnitSearchParameterException {
    if (CollectionUtils.isEmpty(textUnitSearchBody.getRepositoryIds())
        && CollectionUtils.isEmpty(textUnitSearchBody.getRepositoryNames())
        && CollectionUtils.isEmpty(textUnitSearchBody.getTmTextUnitIds())) {
      throw new InvalidTextUnitSearchParameterException(
          "repositoryIds[], repositoryNames[] or tmTextUnitIds[] must be provided");
    }
  }

  TextUnitSearcherParameters textUnitSearchBodyToTextUnitSearcherParameters(
      TextUnitSearchBody textUnitSearchBody) throws InvalidTextUnitSearchParameterException {

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();

    textUnitSearcherParameters.setRepositoryIds(textUnitSearchBody.getRepositoryIds());
    textUnitSearcherParameters.setRepositoryNames(textUnitSearchBody.getRepositoryNames());
    textUnitSearcherParameters.setTmTextUnitIds(textUnitSearchBody.getTmTextUnitIds());
    textUnitSearcherParameters.setName(textUnitSearchBody.getName());
    textUnitSearcherParameters.setSource(
        emptyOrString(textUnitSearchBody.getSource(), textUnitSearchBody.searchType));
    textUnitSearcherParameters.setTarget(
        emptyOrString(textUnitSearchBody.getTarget(), textUnitSearchBody.searchType));
    textUnitSearcherParameters.setAssetPath(textUnitSearchBody.getAssetPath());
    textUnitSearcherParameters.setPluralFormOther(textUnitSearchBody.getPluralFormOther());
    textUnitSearcherParameters.setPluralFormsFiltered(textUnitSearchBody.isPluralFormFiltered());
    textUnitSearcherParameters.setPluralFormsExcluded(textUnitSearchBody.isPluralFormExcluded());
    textUnitSearcherParameters.setSearchType(textUnitSearchBody.getSearchType());
    textUnitSearcherParameters.setLocaleTags(textUnitSearchBody.getLocaleTags());
    textUnitSearcherParameters.setUsedFilter(textUnitSearchBody.getUsedFilter());
    textUnitSearcherParameters.setStatusFilter(textUnitSearchBody.getStatusFilter());
    textUnitSearcherParameters.setDoNotTranslateFilter(
        textUnitSearchBody.getDoNotTranslateFilter());
    textUnitSearcherParameters.setTmTextUnitCreatedBefore(
        textUnitSearchBody.getTmTextUnitCreatedBefore());
    textUnitSearcherParameters.setTmTextUnitCreatedAfter(
        textUnitSearchBody.getTmTextUnitCreatedAfter());
    textUnitSearcherParameters.setBranchId(textUnitSearchBody.getBranchId());
    textUnitSearcherParameters.setLimit(textUnitSearchBody.getLimit());
    textUnitSearcherParameters.setOffset(textUnitSearchBody.getOffset());

    return textUnitSearcherParameters;
  }

  /**
   * To search for the empty string, allow to pass \0000 unicode escape sequence to EXACT search. It
   * will convert that character into the empty string
   */
  String emptyOrString(String string, SearchType searchType) {
    return string != null && SearchType.EXACT.equals(searchType) && string.equals("\\u0000")
        ? ""
        : string;
  }

  /**
   * Creates a TextUnit.
   *
   * <p>Correspond to adding a new TMTextUnitVariant (new translation) in the system and to create
   * the TMTextUnitCurrentVariant (to make the new translation current).
   *
   * @param textUnitDTO data used to update the TMTextUnitCurrentVariant. {@link
   *     TextUnitDTO#getTmTextUnitId()}, {@link TextUnitDTO#getLocaleId()}, {@link
   *     TextUnitDTO#getTarget()} are the only 3 fields that are used for the update.
   * @return the created TextUnit (contains the new translation with its id)
   */
  @Transactional
  @RequestMapping(method = RequestMethod.POST, value = "/api/textunits")
  public TextUnitDTO addTextUnit(@RequestBody TextUnitDTO textUnitDTO) {
    logger.debug("Add TextUnit");
    textUnitDTO.setTarget(NormalizationUtils.normalize(textUnitDTO.getTarget()));
    TMTextUnitCurrentVariant addTMTextUnitCurrentVariant =
        tmService.addTMTextUnitCurrentVariant(
            textUnitDTO.getTmTextUnitId(),
            textUnitDTO.getLocaleId(),
            textUnitDTO.getTarget(),
            textUnitDTO.getTargetComment(),
            textUnitDTO.getStatus(),
            textUnitDTO.isIncludedInLocalizedFile());

    textUnitDTO.setTmTextUnitCurrentVariantId(addTMTextUnitCurrentVariant.getId());
    textUnitDTO.setTmTextUnitVariantId(addTMTextUnitCurrentVariant.getTmTextUnitVariant().getId());
    return textUnitDTO;
  }

  /**
   * Imports batch of text units.
   *
   * <p>Note that the status of the text unit is not taken in account nor the included in localized
   * file attribute. For now, the integrity checker is always applied and is used to determine the
   * {@link TMTextUnitVariant.Status}. TODO later we want to change that to look at the status
   * provided.
   *
   * @param textUnitDTOs
   * @return
   */
  @RequestMapping(method = RequestMethod.POST, value = "/api/textunitsBatch")
  public PollableTask importTextUnitBatch(@RequestBody String string) {

    ImportTextUnitsBatch importTextUnitsBatch = new ImportTextUnitsBatch();

    ObjectMapper objectMapper = ObjectMapper.withNoFailOnUnknownProperties();
    try {
      List<TextUnitDTO> textUnitDTOs =
          objectMapper.readValue(string, new TypeReference<List<TextUnitDTO>>() {});
      importTextUnitsBatch.setTextUnits(textUnitDTOs);
    } catch (Exception e) {
      logger.debug("can't convert to list, try new formatTextUnitBatchImporterServiceTest", e);
      try {
        importTextUnitsBatch = objectMapper.readValue(string, ImportTextUnitsBatch.class);
      } catch (Exception e2) {
        throw new IllegalArgumentException("Can't deserialize text unit batch", e2);
      }
    }

    PollableFuture<Void> pollableFuture =
        textUnitBatchImporterService.asyncImportTextUnits(
            importTextUnitsBatch.getTextUnits(),
            importTextUnitsBatch.isIntegrityCheckSkipped(),
            importTextUnitsBatch.isIntegrityCheckKeepStatusIfFailedAndSameTarget());

    return pollableFuture.getPollableTask();
  }

  /**
   * Deletes the TextUnit.
   *
   * <p>Corresponds to updating the TmTextUnitCurrentVariant entry. This doesn't remove the
   * translation from the system, it just removes it from being the current translation.
   *
   * @param textUnitId TextUnit id (maps to {@link TMTextUnitCurrentVariant#id})
   */
  @RequestMapping(method = RequestMethod.DELETE, value = "/api/textunits/{textUnitId}")
  public void deleteTMTextUnitCurrentVariant(@PathVariable Long textUnitId) {
    logger.debug("Delete textUnitId, id: {}", textUnitId);
    tmTextUnitCurrentVariantService.removeCurrentVariant(textUnitId);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/api/textunits/check")
  public TMTextUnitIntegrityCheckResult checkTMTextUnit(
      @RequestBody TextUnitCheckBody textUnitCheckBody) {
    logger.debug("Checking TextUnit, id: {}", textUnitCheckBody.getTmTextUnitId());

    TMTextUnitIntegrityCheckResult result = new TMTextUnitIntegrityCheckResult();
    try {
      tmTextUnitIntegrityCheckService.checkTMTextUnitIntegrity(
          textUnitCheckBody.getTmTextUnitId(), textUnitCheckBody.getContent());
      result.setCheckResult(true);
    } catch (IntegrityCheckException e) {
      logger.info(
          "Integrity check failed for string with tmTextUnitId: {}, content:\n{}",
          textUnitCheckBody.getTmTextUnitId(),
          textUnitCheckBody.getContent(),
          e);
      result.setCheckResult(false);
      result.setFailureDetail(e.getMessage());
    }

    return result;
  }

  /**
   * Bulk update of text unit statistics information.
   *
   * <p>This creates or updates TMTextUnit Statistics entries with the latest data provided for a
   * specific asset path in a specific repository.
   *
   * @param repositoryName The repository name for the text units in scope.
   * @param assetPath The asset path for the text units in scope.
   * @param textUnitStatistics A list of objects containing information used in matching the
   *     statistics with specific text units and the target information to update.
   * @return A pollable task.
   * @throws RepositoryNameNotFoundException If the name of the repository is not found in the
   *     database.
   * @throws AssetPathNotFoundException If the asset path is not found in the database.
   */
  @RequestMapping(method = RequestMethod.POST, value = "/api/textunits/statistics")
  public PollableTask importStatistics(
      @RequestParam(value = "repositoryName") String repositoryName,
      @RequestParam(value = "assetPath") String assetPath,
      @RequestBody List<ImportTextUnitStatisticsBody> textUnitStatistics)
      throws RepositoryNameNotFoundException, AssetPathNotFoundException {
    logger.debug("Import TextUnit Statistics");

    Repository repository = repositoryRepository.findByName(repositoryName);
    if (repository == null) {
      throw new RepositoryNameNotFoundException(
          String.format("Repository with name '%s' can not be found!", repositoryName));
    }

    Asset asset = assetRepository.findByPathAndRepositoryId(assetPath, repository.getId());
    if (asset == null) {
      throw new AssetPathNotFoundException(
          String.format("Asset with path '%s' can not be found!", assetPath));
    }

    PollableFuture<Void> pollableFuture =
        tmTextUnitStatisticService.importStatistics(
            repository.getSourceLocale(), asset, textUnitStatistics);

    return pollableFuture.getPollableTask();
  }

  @RequestMapping(
      method = RequestMethod.GET,
      value = "/api/assetTextUnits/{assetTextUnitId}/usages")
  public Set<String> getAssetTextUnitUsages(@PathVariable Long assetTextUnitId)
      throws AssetTextUnitWithIdNotFoundException {
    logger.debug("Get usages of asset text unit for id: {}", assetTextUnitId);
    AssetTextUnit assetTextUnit = assetTextUnitRepository.findById(assetTextUnitId).orElse(null);

    if (assetTextUnit == null) {
      throw new AssetTextUnitWithIdNotFoundException(assetTextUnitId);
    }

    return assetTextUnit.getUsages();
  }

  /**
   * Gets the GitBlame information that matches the search parameters.
   *
   * <p>It uses a filter logic. The dataset will be filtered down as more criteria are specified.
   *
   * @param repositoryIds mandatory if repositoryNames or tmTextUnitId not provided
   * @param repositoryNames mandatory if repositoryIds or tmTextUnitId not provided
   * @param tmTextUnitId mandatory if repositoryIds or repositoryNames not provided
   * @param usedFilter optional
   * @param statusFilter optional
   * @param doNotTranslateFilter optional
   * @param limit optional, default 10
   * @param offset optional, default 0
   * @return the GitBlame that matches the search parameters
   * @throws InvalidTextUnitSearchParameterException
   */
  @JsonView(View.GitBlameWithUsage.class)
  @RequestMapping(method = RequestMethod.GET, value = "/api/textunits/gitBlameWithUsages")
  public List<GitBlameWithUsage> getGitBlameWithUsages(
      @RequestParam(value = "repositoryIds[]", required = false) ArrayList<Long> repositoryIds,
      @RequestParam(value = "repositoryNames[]", required = false)
          ArrayList<String> repositoryNames,
      @RequestParam(value = "tmTextUnitId", required = false) Long tmTextUnitId,
      @RequestParam(value = "usedFilter", required = false) UsedFilter usedFilter,
      @RequestParam(value = "statusFilter", required = false) StatusFilter statusFilter,
      @RequestParam(value = "doNotTranslateFilter", required = false) Boolean doNotTranslateFilter,
      @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
      @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset)
      throws InvalidTextUnitSearchParameterException {

    logger.debug("getGitBlameWithUsages");

    if (CollectionUtils.isEmpty(repositoryIds)
        && CollectionUtils.isEmpty(repositoryNames)
        && tmTextUnitId == null) {
      throw new InvalidTextUnitSearchParameterException(
          "repositoryIds[], repositoryNames[] or tmTextUnitId must be provided");
    }

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(repositoryIds);
    textUnitSearcherParameters.setRepositoryNames(repositoryNames);
    textUnitSearcherParameters.setTmTextUnitIds(tmTextUnitId);
    textUnitSearcherParameters.setUsedFilter(usedFilter);
    textUnitSearcherParameters.setStatusFilter(statusFilter);
    textUnitSearcherParameters.setDoNotTranslateFilter(doNotTranslateFilter);

    textUnitSearcherParameters.setForRootLocale(true);
    textUnitSearcherParameters.setPluralFormsFiltered(false);
    textUnitSearcherParameters.setLimit(limit);
    textUnitSearcherParameters.setOffset(offset);

    List<GitBlameWithUsage> gitBlameWithUsages =
        gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);
    return gitBlameWithUsages;
  }

  /**
   * Save the GitBlame information of the text units.
   *
   * @param gitBlameWithUsages
   * @return
   */
  @RequestMapping(method = RequestMethod.POST, value = "/api/textunits/gitBlameWithUsagesBatch")
  public PollableTask saveGitBlameWithUsages(
      @RequestBody List<GitBlameWithUsage> gitBlameWithUsages) {
    logger.debug("saveGitBlameWithUsages");
    PollableFuture<Void> pollableFuture =
        gitBlameService.saveGitBlameWithUsages(gitBlameWithUsages);
    return pollableFuture.getPollableTask();
  }
}
