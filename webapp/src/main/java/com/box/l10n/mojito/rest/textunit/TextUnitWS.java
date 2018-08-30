package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.entity.*;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckException;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitIntegrityCheckService;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.SearchType;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitAndWordCount;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;

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
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    @Autowired
    TMTextUnitIntegrityCheckService tmTextUnitIntegrityCheckService;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

    @Autowired
    TextUnitBatchImporterService textUnitBatchImporterService;

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
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "target", required = false) String target,
            @RequestParam(value = "assetPath", required = false) String assetPath,
            @RequestParam(value = "pluralFormOther", required = false) String pluralFormOther,
            @RequestParam(value = "pluralFormFiltered", required = false, defaultValue = "true") boolean pluralFormFiltered,
            @RequestParam(value = "searchType", required = false, defaultValue = "EXACT") SearchType searchType,
            @RequestParam(value = "localeTags[]", required = false) ArrayList<String> localeTags,
            @RequestParam(value = "usedFilter", required = false) UsedFilter usedFilter,
            @RequestParam(value = "statusFilter", required = false) StatusFilter statusFilter,
            @RequestParam(value = "doNotTranslateFilter", required = false) Boolean doNotTranslateFilter,
            @RequestParam(value = "tmTextUnitCreatedBefore", required = false) DateTime tmTextUnitCreatedBefore,
            @RequestParam(value = "tmTextUnitCreatedAfter", required = false) DateTime tmTextUnitCreatedAfter,
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset) throws InvalidTextUnitSearchParameterException {

        TextUnitSearcherParameters textUnitSearcherParameters = queryParamsToTextUnitSearcherParameters(repositoryIds,
                repositoryNames, name, source, target, assetPath, pluralFormOther, pluralFormFiltered, searchType, localeTags, usedFilter, statusFilter, doNotTranslateFilter, tmTextUnitCreatedBefore, tmTextUnitCreatedAfter);
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
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "target", required = false) String target,
            @RequestParam(value = "assetPath", required = false) String assetPath,
            @RequestParam(value = "pluralFormOther", required = false) String pluralFormOther,
            @RequestParam(value = "pluralFormFiltered", required = false, defaultValue = "true") boolean pluralFormFiltered,
            @RequestParam(value = "searchType", required = false, defaultValue = "EXACT") SearchType searchType,
            @RequestParam(value = "localeTags[]", required = false) ArrayList<String> localeTags,
            @RequestParam(value = "usedFilter", required = false) UsedFilter usedFilter,
            @RequestParam(value = "statusFilter", required = false) StatusFilter statusFilter,
            @RequestParam(value = "doNotTranslateFilter", required = false) Boolean doNotTranslateFilter,
            @RequestParam(value = "tmTextUnitCreatedBe                                                           fore", required = false) DateTime tmTextUnitCreatedBefore,
            @RequestParam(value = "tmTextUnitCreatedAfter", required = false) DateTime tmTExtunitCreatedAfter) throws InvalidTextUnitSearchParameterException {

        TextUnitSearcherParameters textUnitSearcherParameters = queryParamsToTextUnitSearcherParameters(
                repositoryIds, repositoryNames, name, source, target,
                assetPath, pluralFormOther, pluralFormFiltered, searchType,
                localeTags, usedFilter, statusFilter, doNotTranslateFilter,
                tmTextUnitCreatedBefore, tmTExtunitCreatedAfter);

        TextUnitAndWordCount countTextUnitAndWordCount = textUnitSearcher.countTextUnitAndWordCount(textUnitSearcherParameters);
        return countTextUnitAndWordCount;
    }

    TextUnitSearcherParameters queryParamsToTextUnitSearcherParameters(
            ArrayList<Long> repositoryIds,
            ArrayList<String> repositoryNames,
            String name,
            String source,
            String target,
            String assetPath,
            String pluralFormOther,
            boolean pluralFormFiltered,
            SearchType searchType,
            ArrayList<String> localeTags,
            UsedFilter usedFilter,
            StatusFilter statusFilter,
            Boolean doNotTranslateFilter,
            DateTime tmTextUnitCreatedBefore,
            DateTime tmTextUnitCreatedAfter) throws InvalidTextUnitSearchParameterException {

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();

        if (CollectionUtils.isEmpty(repositoryIds) && CollectionUtils.isEmpty(repositoryNames)) {
            throw new InvalidTextUnitSearchParameterException("Repository ids or names must be provided");
        }

        textUnitSearcherParameters.setRepositoryIds(repositoryIds); //
        textUnitSearcherParameters.setRepositoryNames(repositoryNames);
        textUnitSearcherParameters.setName(name);
        textUnitSearcherParameters.setSource(source);
        textUnitSearcherParameters.setTarget(target);
        textUnitSearcherParameters.setAssetPath(assetPath);
        textUnitSearcherParameters.setPluralFormOther(pluralFormOther);
        textUnitSearcherParameters.setPluralFormsFiltered(pluralFormFiltered);
        textUnitSearcherParameters.setSearchType(searchType);
        textUnitSearcherParameters.setRootLocaleExcluded(false);
        textUnitSearcherParameters.setLocaleTags(localeTags);
        textUnitSearcherParameters.setUsedFilter(usedFilter); // only want used
        textUnitSearcherParameters.setStatusFilter(statusFilter);
        textUnitSearcherParameters.setDoNotTranslateFilter(doNotTranslateFilter);
        textUnitSearcherParameters.setTmTextUnitCreatedBefore(tmTextUnitCreatedBefore);
        textUnitSearcherParameters.setTmTextUnitCreatedAfter(tmTextUnitCreatedAfter);

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
            List<TextUnitDTO>  textUnitDTOs = objectMapper.readValue(string, new TypeReference<List<TextUnitDTO>>(){});
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
     * Corresponds to removing the TmTextUnitCurrentVariant entry. This doesn't
     * remove the translation from the system, it just removes it from being the
     * current translation.
     *
     * @param textUnitId TextUnit id (maps to
     *                   {@link TMTextUnitCurrentVariant#id})
     */
    @Transactional
    @RequestMapping(method = RequestMethod.DELETE, value = "/api/textunits/{textUnitId}")
    public void deleteTMTextUnitCurrentVariant(@PathVariable Long textUnitId) {
        logger.debug("Delete TextUnit, id: {}", textUnitId);

        TMTextUnitCurrentVariant tmtucv = tmTextUnitCurrentVariantRepository.findOne(textUnitId);

        if (tmtucv == null) {
            logger.debug("Already removed, do nothing");
        } else {
            logger.debug("Remove tmTextUnitCurrentVariantRepository: {}", tmtucv.getId());
            tmTextUnitCurrentVariantRepository.delete(tmtucv.getId());
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/api/textunits/check")
    public TMTextUnitIntegrityCheckResult checkTMTextUnit(@RequestParam(value = "textUnitId") Long textUnitId,
                                                          @RequestParam(value = "contentToCheck") String contentToCheck) {
        logger.debug("Checking TextUnit, id: {}", textUnitId);

        TMTextUnitIntegrityCheckResult result = new TMTextUnitIntegrityCheckResult();
        try {
            tmTextUnitIntegrityCheckService.checkTMTextUnitIntegrity(textUnitId, contentToCheck);
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
        AssetTextUnit assetTextUnit = assetTextUnitRepository.findOne(assetTextUnitId);

        if (assetTextUnit == null) {
            throw new AssetTextUnitWithIdNotFoundException(assetTextUnitId);
        }

        return assetTextUnit.getUsages();
    }


    @RequestMapping(method = RequestMethod.GET, value = "/api/textUnit/{textUnitId}/gitBlame")
    public String getGitBlameForTmTextUnit(@PathVariable Long tmTextUnit) throws AssetTextUnitWithIdNotFoundException {
//        logger.debug("Get usages of asset text unit for id: {}", assetTextUnitId);
        return "";
    }

    @RequestMapping(method = RequestMethod.GET, value = "/api/gitBlames")
    public List<GitBlameWithUsages> getGitBlames(@PathVariable Long tmTextUnit,
                                                 @RequestParam(value = "repositoryIds[]", required = true) ArrayList<Long> repositoryIds,
                                                 @RequestParam(value = "textUnitIds[]", required = false) ArrayList<Long> textUnitIds,
                                                 @RequestParam(value = "blamedFilter", required = false) Boolean blamedFilter,
                                                 @RequestParam(value = "fetchUsages", required = false) boolean fetchUsages
    ) throws AssetTextUnitWithIdNotFoundException {

//        logger.debug("Get usages of asset text unit for id: {}", assetTextUnitId);


        GitBlameWithUsages gitBlameWithUsages = new GitBlameWithUsages();
        gitBlameWithUsages.setTmTextUnitId(1L);
        gitBlameWithUsages.setGitBlame(new GitBlame());
        gitBlameWithUsages.getGitBlame().setAuthorEmail("");


        if (fetchUsages) {
            logger.debug("Fetch usages");
        }


        // update the text unit searcher to return the blame information...


        // text unit that are not blamed.
        // -> get used text units
        // -> get entries without blame
        // -> optionally fetch usages


        return null;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/api/textUnitToBlame/gitInfo")
    public void saveGitInfoForTextUnits (@RequestBody GitInfoForTextUnits gitInfoForTextUnits) {
//           logger.debug("saveGitInfoForTextUnits");
    }

    @RequestMapping(method = RequestMethod.GET, value = "/api/textUnitToBlame")
    public List<TextUnitWithUsage> getTextUnitWithUsages(@RequestParam(value = "repositoryId", required = true) Long repositoryId) {

        List<TextUnitWithUsage> textUnitWithUsages = new ArrayList<>();
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repositoryId);
        textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
        textUnitSearcherParameters.setForRootLocale(true);
        textUnitSearcherParameters.setPluralFormsFiltered(false);
        List<TextUnitDTO> textUnitDTOS = textUnitSearcher.search(textUnitSearcherParameters);

        for (TextUnitDTO textUnitDTO : textUnitDTOS) {
            AssetTextUnit assetTextUnit = assetTextUnitRepository.findOne(textUnitDTO.getAssetTextUnitId());
            TextUnitWithUsage textUnitWithUsage = new TextUnitWithUsage();
            textUnitWithUsage.setTextUnitName(assetTextUnit.getName());
            textUnitWithUsage.setUsages(new ArrayList<>(assetTextUnit.getUsages())); // android will have no usages
            textUnitWithUsage.setTextUnitId(textUnitDTO.getTmTextUnitId());
            textUnitWithUsages.add(textUnitWithUsage);
        }

        return textUnitWithUsages;
    }
}
