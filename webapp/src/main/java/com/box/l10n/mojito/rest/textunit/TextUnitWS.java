package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckException;
import com.box.l10n.mojito.service.gitblame.GitBlameService;
import com.box.l10n.mojito.service.gitblame.GitBlameWithUsage;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantService;
import com.box.l10n.mojito.service.tm.TMTextUnitHistoryService;
import com.box.l10n.mojito.service.tm.TMTextUnitIntegrityCheckService;
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
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Webservices for the workbench. Allows to search for TextUnits and
 * add/update/delete translations.
 *
 * @author Jean
 */
@RestController
public class TextUnitWS {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TextUnitWS.class);

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    TMService tmService;

    @Autowired
    TMTextUnitHistoryService tmHistoryService;

    @Autowired
    TMTextUnitCurrentVariantService tmTextUnitCurrentVariantService;

    @Autowired
    TMTextUnitIntegrityCheckService tmTextUnitIntegrityCheckService;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

    @Autowired
    TextUnitBatchImporterService textUnitBatchImporterService;

    @Autowired
    GitBlameService gitBlameService;

    @Autowired
    LocaleService localeService;

    /**
     * Gets the TextUnits that matches the search parameters.
     *
     * It uses a filter logic. The dataset will be filtered down as more
     * criteria are specified (search for specific locales, string
     * name|target|source, etc).
     *
     * @param repositoryIds           mandatory if repositoryNames not provided
     * @param repositoryNames         mandatory if repositoryIds not provided
     * @param name                    optional
     * @param source                  optional
     * @param target                  optional
     * @param assetPath               optional
     * @param pluralFormOther         optional
     * @param pluralFormFiltered      optional
     * @param searchType              optional, default is EXACT match
     * @param localeTags              optional
     * @param usedFilter              optional
     * @param statusFilter            optional
     * @param doNotTranslateFilter    optional
     * @param tmTextUnitCreatedBefore optional
     * @param tmTextUnitCreatedAfter  optional
     * @param limit                   optional, default 10
     * @param offset                  optional, default 0
     * @return the TextUnits that matches the search parameters
     * @throws InvalidTextUnitSearchParameterException
     */
    @RequestMapping(method = RequestMethod.GET, value = "/api/textunits")
    @ResponseStatus(HttpStatus.OK)
    public List<TextUnitDTO> getTextUnits(
            @RequestParam(value = "repositoryIds[]", required = false) ArrayList<Long> repositoryIds,
            @RequestParam(value = "repositoryNames[]", required = false) ArrayList<String> repositoryNames,
            @RequestParam(value = "tmTextUnitIds[]", required = false) ArrayList<Long> tmTextUnitIds,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "target", required = false) String target,
            @RequestParam(value = "assetPath", required = false) String assetPath,
            @RequestParam(value = "pluralFormOther", required = false) String pluralFormOther,
            @RequestParam(value = "pluralFormFiltered", required = false, defaultValue = "true") boolean pluralFormFiltered,
            @RequestParam(value = "pluralFormExcluded", required = false, defaultValue = "false") boolean pluralFormExcluded,
            @RequestParam(value = "searchType", required = false, defaultValue = "EXACT") SearchType searchType,
            @RequestParam(value = "localeTags[]", required = false) ArrayList<String> localeTags,
            @RequestParam(value = "usedFilter", required = false) UsedFilter usedFilter,
            @RequestParam(value = "statusFilter", required = false) StatusFilter statusFilter,
            @RequestParam(value = "doNotTranslateFilter", required = false) Boolean doNotTranslateFilter,
            @RequestParam(value = "tmTextUnitCreatedBefore", required = false) DateTime tmTextUnitCreatedBefore,
            @RequestParam(value = "tmTextUnitCreatedAfter", required = false) DateTime tmTextUnitCreatedAfter,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset) throws InvalidTextUnitSearchParameterException {

        TextUnitSearcherParameters textUnitSearcherParameters = queryParamsToTextUnitSearcherParameters(
                repositoryIds,   repositoryNames, tmTextUnitIds,
                name, source, target, assetPath, pluralFormOther,
                pluralFormFiltered, pluralFormExcluded, localeTags,
                usedFilter, statusFilter, doNotTranslateFilter, tmTextUnitCreatedBefore,
                tmTextUnitCreatedAfter, branchId, searchType
        );
        textUnitSearcherParameters.setLimit(limit);
        textUnitSearcherParameters.setOffset(offset);
        List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);

        return search;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/api/textunits/count")
    @ResponseStatus(HttpStatus.OK)
    public TextUnitAndWordCount getTextUnitsCount(
            @RequestParam(value = "repositoryIds[]", required = false) ArrayList<Long> repositoryIds,
            @RequestParam(value = "repositoryNames[]", required = false) ArrayList<String> repositoryNames,
            @RequestParam(value = "tmTextUnitIds[]", required = false) ArrayList<Long> tmTextUnitIds,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "target", required = false) String target,
            @RequestParam(value = "assetPath", required = false) String assetPath,
            @RequestParam(value = "pluralFormOther", required = false) String pluralFormOther,
            @RequestParam(value = "pluralFormFiltered", required = false, defaultValue = "true") boolean pluralFormFiltered,
            @RequestParam(value = "pluralFormExcluded", required = false, defaultValue = "false") boolean pluralFormExcluded,
            @RequestParam(value = "searchType", required = false, defaultValue = "EXACT") SearchType searchType,
            @RequestParam(value = "localeTags[]", required = false) ArrayList<String> localeTags,
            @RequestParam(value = "usedFilter", required = false) UsedFilter usedFilter,
            @RequestParam(value = "statusFilter", required = false) StatusFilter statusFilter,
            @RequestParam(value = "doNotTranslateFilter", required = false) Boolean doNotTranslateFilter,
            @RequestParam(value = "tmTextUnitCreatedBefore", required = false) DateTime tmTextUnitCreatedBefore,
            @RequestParam(value = "tmTextUnitCreatedAfter", required = false) DateTime tmTextUnitCreatedAfter,
            @RequestParam(value = "branchId", required = false) Long branchId) throws InvalidTextUnitSearchParameterException {

        TextUnitSearcherParameters textUnitSearcherParameters = queryParamsToTextUnitSearcherParameters(
                repositoryIds, repositoryNames, tmTextUnitIds, name, source, target,
                assetPath, pluralFormOther, pluralFormFiltered, pluralFormExcluded,
                localeTags, usedFilter, statusFilter, doNotTranslateFilter, tmTextUnitCreatedBefore,
                tmTextUnitCreatedAfter, branchId, searchType
        );

        TextUnitAndWordCount countTextUnitAndWordCount = textUnitSearcher.countTextUnitAndWordCount(textUnitSearcherParameters);
        return countTextUnitAndWordCount;
    }

    /**
     * Gets the translation history for a given text unit for a particular locale.
     *
     * @param textUnitId            required
     * @param bcp47Tag              required
     *
     * @return the translations that matches the search parameters
     * @throws InvalidTextUnitSearchParameterException
     */
    @RequestMapping(method = RequestMethod.GET, value = "/api/textunits/{textUnitId}/history")
    @ResponseStatus(HttpStatus.OK)
    @JsonView(View.TranslationHistorySummary.class)
    public List<TMTextUnitVariant> getTextUnitHistory(
            @PathVariable Long textUnitId,
            @RequestParam(value = "bcp47Tag", required = true) String bcp47Tag) throws InvalidTextUnitSearchParameterException {

        Locale locale = localeService.findByBcp47Tag(bcp47Tag);
        return tmHistoryService.findHistory(textUnitId, locale.getId());
    }

    TextUnitSearcherParameters queryParamsToTextUnitSearcherParameters(
            ArrayList<Long> repositoryIds,
            ArrayList<String> repositoryNames,
            ArrayList<Long> tmTextUnitIds,
            String name,
            String source,
            String target,
            String assetPath,
            String pluralFormOther,
            boolean pluralFormFiltered,
            boolean pluralFormExcluded,
            ArrayList<String> localeTags,
            UsedFilter usedFilter,
            StatusFilter statusFilter,
            Boolean doNotTranslateFilter,
            DateTime tmTextUnitCreatedBefore,
            DateTime tmTextUnitCreatedAfter,
            Long branchId,
            SearchType searchType) throws InvalidTextUnitSearchParameterException {

        if (CollectionUtils.isEmpty(repositoryIds)
                && CollectionUtils.isEmpty(repositoryNames)
                && CollectionUtils.isEmpty(tmTextUnitIds)) {
            throw new InvalidTextUnitSearchParameterException("Repository ids, repository names or tm text unit ids must be provided");
        }

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();

        textUnitSearcherParameters.setRepositoryIds(repositoryIds);
        textUnitSearcherParameters.setRepositoryNames(repositoryNames);
        textUnitSearcherParameters.setTmTextUnitIds(tmTextUnitIds);
        textUnitSearcherParameters.setName(name);
        textUnitSearcherParameters.setSource(source);
        textUnitSearcherParameters.setTarget(target);
        textUnitSearcherParameters.setAssetPath(assetPath);
        textUnitSearcherParameters.setPluralFormOther(pluralFormOther);
        textUnitSearcherParameters.setPluralFormsFiltered(pluralFormFiltered);
        textUnitSearcherParameters.setPluralFormsExcluded(pluralFormExcluded);
        textUnitSearcherParameters.setSearchType(searchType);
        textUnitSearcherParameters.setRootLocaleExcluded(false);
        textUnitSearcherParameters.setLocaleTags(localeTags);
        textUnitSearcherParameters.setUsedFilter(usedFilter);
        textUnitSearcherParameters.setStatusFilter(statusFilter);
        textUnitSearcherParameters.setDoNotTranslateFilter(doNotTranslateFilter);
        textUnitSearcherParameters.setTmTextUnitCreatedBefore(tmTextUnitCreatedBefore);
        textUnitSearcherParameters.setTmTextUnitCreatedAfter(tmTextUnitCreatedAfter);
        textUnitSearcherParameters.setBranchId(branchId);

        return textUnitSearcherParameters;
    }

    /**
     * Creates a TextUnit.
     *
     * Correspond to adding a new TMTextUnitVariant (new translation) in the
     * system and to create the TMTextUnitCurrentVariant (to make the new
     * translation current).
     *
     * @param textUnitDTO data used to update the TMTextUnitCurrentVariant. {@link TextUnitDTO#getTmTextUnitId()},
     *                    {@link TextUnitDTO#getLocaleId()}, {@link TextUnitDTO#getTarget()} are
     *                    the only 3 fields that are used for the update.
     * @return the created TextUnit (contains the new translation with its id)
     */
    @Transactional
    @RequestMapping(method = RequestMethod.POST, value = "/api/textunits")
    public TextUnitDTO addTextUnit(@RequestBody TextUnitDTO textUnitDTO) {
        logger.debug("Add TextUnit");
        textUnitDTO.setTarget(NormalizationUtils.normalize(textUnitDTO.getTarget()));
        TMTextUnitCurrentVariant addTMTextUnitCurrentVariant = tmService.addTMTextUnitCurrentVariant(
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
     * Note that the status of the text unit is not taken in account nor the included in localized file attribute. For
     * now, the integrity checker is always applied and is used to determine the
     * {@link TMTextUnitVariant.Status}. TODO later we want to change that to look at the status provided.
     *
     * @param textUnitDTOs
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, value = "/api/textunitsBatch")
    public PollableTask importTextUnitBatch(@RequestBody String string) {

        ImportTextUnitsBatch importTextUnitsBatch = new ImportTextUnitsBatch();

        // TODO remove when  clients are migrated
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            List<TextUnitDTO> textUnitDTOs = objectMapper.readValue(string, new TypeReference<List<TextUnitDTO>>() {
            });
            importTextUnitsBatch.setTextUnits(textUnitDTOs);
        } catch (Exception e) {
            logger.debug("can't convert to list, try new formatTextUnitBatchImporterServiceTest", e);
            try {
                importTextUnitsBatch = objectMapper.readValue(string, ImportTextUnitsBatch.class);
            } catch (Exception e2) {
                throw new IllegalArgumentException("Can't deserialize text unit batch", e2);
            }
        }

        PollableFuture pollableFuture = textUnitBatchImporterService.asyncImportTextUnits(
                importTextUnitsBatch.getTextUnits(),
                importTextUnitsBatch.isIntegrityCheckSkipped(),
                importTextUnitsBatch.isIntegrityCheckKeepStatusIfFailedAndSameTarget());

        return pollableFuture.getPollableTask();
    }

    /**
     * Deletes the TextUnit.
     *
     * Corresponds to updating the TmTextUnitCurrentVariant entry. This doesn't
     * remove the translation from the system, it just removes it from being the
     * current translation.
     *
     * @param textUnitId TextUnit id (maps to
     *                   {@link TMTextUnitCurrentVariant#id})
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/api/textunits/{textUnitId}")
    public void deleteTMTextUnitCurrentVariant(@PathVariable Long textUnitId) {
        logger.debug("Delete textUnitId, id: {}", textUnitId);
        tmTextUnitCurrentVariantService.removeCurrentVariant(textUnitId);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/api/textunits/check")
    public TMTextUnitIntegrityCheckResult checkTMTextUnit(@RequestBody TextUnitCheckBody textUnitCheckBody) {
        logger.debug("Checking TextUnit, id: {}", textUnitCheckBody.getTmTextUnitId());

        TMTextUnitIntegrityCheckResult result = new TMTextUnitIntegrityCheckResult();
        try {
            tmTextUnitIntegrityCheckService.checkTMTextUnitIntegrity(textUnitCheckBody.getTmTextUnitId(), textUnitCheckBody.getContent());
            result.setCheckResult(true);
        } catch (IntegrityCheckException e) {
            result.setCheckResult(false);
            result.setFailureDetail(e.getMessage());
        }

        return result;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/api/assetTextUnits/{assetTextUnitId}/usages")
    public Set<String> getAssetTextUnitUsages(@PathVariable Long assetTextUnitId) throws AssetTextUnitWithIdNotFoundException {
        logger.debug("Get usages of asset text unit for id: {}", assetTextUnitId);
        AssetTextUnit assetTextUnit = assetTextUnitRepository.findById(assetTextUnitId).orElse(null);

        if (assetTextUnit == null) {
            throw new AssetTextUnitWithIdNotFoundException(assetTextUnitId);
        }

        return assetTextUnit.getUsages();
    }

    /**
     * Gets the GitBlame information that matches the search parameters.
     * <p>
     * It uses a filter logic. The dataset will be filtered down as more
     * criteria are specified.
     *
     * @param repositoryIds           mandatory if repositoryNames or tmTextUnitId not provided
     * @param repositoryNames         mandatory if repositoryIds or tmTextUnitId  not provided
     * @param tmTextUnitId            mandatory if repositoryIds or repositoryNames not provided
     * @param usedFilter              optional
     * @param statusFilter            optional
     * @param doNotTranslateFilter    optional
     * @param limit                   optional, default 10
     * @param offset                  optional, default 0
     * @return the GitBlame that matches the search parameters
     * @throws InvalidTextUnitSearchParameterException
     */
    @JsonView(View.GitBlameWithUsage.class)
    @RequestMapping(method = RequestMethod.GET, value = "/api/textunits/gitBlameWithUsages")
    public List<GitBlameWithUsage> getGitBlameWithUsages(@RequestParam(value = "repositoryIds[]", required = false) ArrayList<Long> repositoryIds,
                                                         @RequestParam(value = "repositoryNames[]", required = false) ArrayList<String> repositoryNames,
                                                         @RequestParam(value = "tmTextUnitId", required = false) Long tmTextUnitId,
                                                         @RequestParam(value = "usedFilter", required = false) UsedFilter usedFilter,
                                                         @RequestParam(value = "statusFilter", required = false) StatusFilter statusFilter,
                                                         @RequestParam(value = "doNotTranslateFilter", required = false) Boolean doNotTranslateFilter,
                                                         @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                                                         @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset) throws InvalidTextUnitSearchParameterException {

        logger.debug("getGitBlameWithUsages");

        if (CollectionUtils.isEmpty(repositoryIds) && CollectionUtils.isEmpty(repositoryNames) && tmTextUnitId == null) {
            throw new InvalidTextUnitSearchParameterException("Repository ids, repository names or tmTextUnitId must be provided");
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

        List<GitBlameWithUsage> gitBlameWithUsages = gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);
        return gitBlameWithUsages;
    }

    /**
     * Save the GitBlame information of the text units.
     *
     * @param gitBlameWithUsages
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, value = "/api/textunits/gitBlameWithUsagesBatch")
    public PollableTask saveGitBlameWithUsages(@RequestBody List<GitBlameWithUsage> gitBlameWithUsages) {
        logger.debug("saveGitBlameWithUsages");
        PollableFuture pollableFuture = gitBlameService.saveGitBlameWithUsages(gitBlameWithUsages);
        return pollableFuture.getPollableTask();
    }
}
