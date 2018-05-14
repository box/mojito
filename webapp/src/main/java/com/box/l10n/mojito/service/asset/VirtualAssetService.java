package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.AssetTextUnitToTMTextUnit;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.service.NormalizationUtils;
import static com.box.l10n.mojito.service.asset.AssetTextUnitSpecification.assetExtractionIdEquals;
import static com.box.l10n.mojito.service.asset.AssetTextUnitSpecification.doNotTranslateEquals;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.AssetTextUnitToTMTextUnitRepository;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.leveraging.LeveragerByContentForSourceLeveraging;
import com.box.l10n.mojito.service.leveraging.LeveragerByTmTextUnit;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TranslatorWithInheritance;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.SearchType;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import com.google.common.base.Strings;
import com.ibm.icu.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import static org.springframework.data.jpa.domain.Specifications.where;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to manage virtual assets.
 *
 * @author jaurambault
 */
@Service
public class VirtualAssetService {

    /**
     * logger
     */
    static Logger logger = getLogger(VirtualAssetService.class);

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetExtractionService assetExtractionService;

    @Autowired
    AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

    @Autowired
    AssetExtractionRepository assetExtractionRepository;

    @Autowired
    LeveragerByContentForSourceLeveraging leveragerByContentForSourceLeveraging;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    TMService tmService;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    RepositoryLocaleRepository repositoryLocaleRepository;

    @Autowired
    TextUnitBatchImporterService textUnitBatchImporterService;
    
    @Autowired
    VirtualTextUnitBatchUpdaterService virtualTextUnitBatchUpdaterService;
    
    @Autowired
    LocaleService localeService;

    @Transactional
    public VirtualAsset createOrUpdateVirtualAsset(VirtualAsset virtualAsset) throws VirtualAssetRequiredException {

        logger.debug("Create or update virtual asset with path: {}", virtualAsset.getPath());
        Asset asset = assetRepository.findByPathAndRepositoryId(virtualAsset.getPath(), virtualAsset.getRepositoryId());

        if (asset != null && !asset.getVirtual()) {
            throw new VirtualAssetRequiredException("Standard asset can't be modify");
        } else if (asset != null) {
            logger.debug("Virtual asset exists, update it");
            asset.setDeleted(virtualAsset.getDeleted());
            asset = assetRepository.save(asset);
        } else {
            logger.debug("No existing virtual asset, create one");
            asset = new Asset();
            asset.setRepository(repositoryRepository.getOne(virtualAsset.getRepositoryId()));
            asset.setPath(virtualAsset.getPath());
            asset.setContent("");
            asset.setContentMd5(asset.getContent());
            asset.setVirtual(Boolean.TRUE);

            logger.debug("Create a default AssetExtraction for virtual asset");
            AssetExtraction createAssetExtraction = assetExtractionService.createAssetExtraction(asset, null);
            asset.setLastSuccessfulAssetExtraction(createAssetExtraction);

            asset = assetRepository.save(asset);
        }

        virtualAsset.setId(asset.getId());
        return virtualAsset;
    }

    @Transactional
    public List<VirtualAssetTextUnit> getTextUnits(long assetId, Boolean doNotTranslateFilter) throws VirtualAssetRequiredException {

        logger.debug("Get textunit for virtual asset: {}", assetId);

        List<VirtualAssetTextUnit> virtualAssetTextUnits = new ArrayList<>();

        Asset asset = getVirtualAsset(assetId);
        Long lastSuccessfulAssetExtractionId = asset.getLastSuccessfulAssetExtraction().getId();
        List<AssetTextUnit> findByAssetExtractionAssetId = findByAssetExtractionAssetIdAndDoNotTranslateFilter(lastSuccessfulAssetExtractionId, doNotTranslateFilter);

        for (AssetTextUnit assetTextUnit : findByAssetExtractionAssetId) {
            VirtualAssetTextUnit textUnitForVirtualAsset = convertAssetTextUnitToVirtualAssetTextUnit(assetTextUnit);
            virtualAssetTextUnits.add(textUnitForVirtualAsset);
        }

        return virtualAssetTextUnits;
    }

    List<AssetTextUnit> findByAssetExtractionAssetIdAndDoNotTranslateFilter(Long assetExtractionId, Boolean doNotTranslateFilter) {
        return assetTextUnitRepository.findAll(
                where(assetExtractionIdEquals(assetExtractionId)).and(ifParamNotNull(doNotTranslateEquals(doNotTranslateFilter))),
                new Sort(Sort.Direction.ASC, "name")
        );
    }

    @Transactional
    public List<VirtualAssetTextUnit> getLocalizedTextUnits(
            long assetId,
            long localeId,
            InheritanceMode inheritanceMode) throws VirtualAssetRequiredException {

        logger.debug("Get localized virtual asset: {} for locale: {}", assetId, localeId);
        List<VirtualAssetTextUnit> virtualAssetTextUnits;

        Asset asset = getVirtualAsset(assetId);
        RepositoryLocale repositoryLocale = repositoryLocaleRepository.findByRepositoryIdAndLocaleId(asset.getRepository().getId(), localeId);

        if (repositoryLocale.getParentLocale() == null) {
            logger.debug("Getting text units for the root locale");
            virtualAssetTextUnits = getTextUnits(assetId, null);
        } else {
            virtualAssetTextUnits = getLoalizedTextUnitsForTargetLocale(asset, repositoryLocale, inheritanceMode);
        }

        return virtualAssetTextUnits;
    }

    List<VirtualAssetTextUnit> getLoalizedTextUnitsForTargetLocale(
            Asset asset,
            RepositoryLocale repositoryLocale,
            InheritanceMode inheritanceMode) throws VirtualAssetRequiredException {

        logger.debug("Get localized virtual asset for target locale");
        List<VirtualAssetTextUnit> virtualAssetTextUnits = new ArrayList<>();

        Long lastSuccessfulAssetExtractionId = asset.getLastSuccessfulAssetExtraction().getId();
        List<AssetTextUnit> findByAssetExtractionAssetId = assetTextUnitRepository.findByAssetExtractionIdOrderByNameAsc(lastSuccessfulAssetExtractionId);

        TranslatorWithInheritance translatorWithInheritance = new TranslatorWithInheritance(asset, repositoryLocale, inheritanceMode);

        for (AssetTextUnit assetTextUnit : findByAssetExtractionAssetId) {

            String translation = translatorWithInheritance.getTranslation(
                    assetTextUnit.getName(),
                    assetTextUnit.getContent(),
                    assetTextUnit.getMd5()
            );

            if (translation == null && InheritanceMode.REMOVE_UNTRANSLATED.equals(inheritanceMode)) {
                logger.debug("Remove untranslated text unit");
            } else {
                logger.debug("Set translation for text unit with name: {}, translation: {}", assetTextUnit.getName(), translation);

                VirtualAssetTextUnit virtualAssetTextUnit = convertAssetTextUnitToVirtualAssetTextUnit(assetTextUnit);
                virtualAssetTextUnit.setContent(translation);
                if (assetTextUnit.getPluralForm() != null) {
                    virtualAssetTextUnit.setPluralForm(assetTextUnit.getPluralForm().getName());
                }
                virtualAssetTextUnit.setPluralFormOther(assetTextUnit.getPluralFormOther());
                virtualAssetTextUnits.add(virtualAssetTextUnit);
            }
        }

        return virtualAssetTextUnits;
    }

    @Pollable(async = true)
    public PollableFuture addTextUnits(long assetId, List<VirtualAssetTextUnit> virtualAssetTextUnits) throws VirtualAssetRequiredException {
        Asset asset = getVirtualAsset(assetId);
        virtualTextUnitBatchUpdaterService.updateTextUnits(asset, virtualAssetTextUnits, false);
        return new PollableFutureTaskResult<>();
    }

    @Pollable(async = true)
    public PollableFuture replaceTextUnits(long assetId, List<VirtualAssetTextUnit> virtualAssetTextUnits) throws VirtualAssetRequiredException {
        Asset asset = getVirtualAsset(assetId);
        virtualTextUnitBatchUpdaterService.updateTextUnits(asset, virtualAssetTextUnits, true);
        return new PollableFutureTaskResult<>();
    }

    public PollableFuture importLocalizedTextUnits(
            long assetId,
            long localeId,
            List<VirtualAssetTextUnit> textUnitForVirtualAssets)
            throws VirtualAssetRequiredException, VirutalAssetMissingTextUnitException {

        logger.debug("Add text unit variants ({}) to virtual assetId: {}", textUnitForVirtualAssets.size(), assetId);

        List<TextUnitDTO> textUnitDTOs = new ArrayList<>();

        Asset asset = assetRepository.findOne(assetId);
        Locale locale = localeService.findById(localeId);

        for (VirtualAssetTextUnit textUnitForVirtualAsset : textUnitForVirtualAssets) {
            TextUnitDTO textUnitDTO = new TextUnitDTO();
            textUnitDTO.setRepositoryName(asset.getRepository().getName());
            textUnitDTO.setAssetPath(asset.getPath());
            textUnitDTO.setTargetLocale(locale.getBcp47Tag());
            textUnitDTO.setName(textUnitForVirtualAsset.getName());
            textUnitDTO.setTarget(textUnitForVirtualAsset.getContent());
            textUnitDTO.setComment(textUnitForVirtualAsset.getComment());
            textUnitDTOs.add(textUnitDTO);
        }

        return textUnitBatchImporterService.asyncImportTextUnits(textUnitDTOs);
    }

    @Transactional
    public void deleteTextUnit(Long assetId, String name) {
        Asset asset = assetRepository.findOne(assetId);
        deleteTextUnits(asset, Arrays.asList(name));
    }

    public void deleteTextUnits(Asset asset, List<String> names) {

        Long assetExtractionId = asset.getLastSuccessfulAssetExtraction().getId();

        for (String name : names) {
            logger.debug("Try deleting text unit with name: {} from asset: {}", name, asset.getPath());

            AssetTextUnit assetTextUnit = assetTextUnitRepository.findByAssetExtractionIdAndName(assetExtractionId, name);

            if (assetTextUnit != null) {
                logger.debug("Asset text unit found, perform delete");
                assetTextUnitToTMTextUnitRepository.deleteByAssetTextUnitId(assetTextUnit.getId());
                assetTextUnitRepository.delete(assetTextUnit);
            } else {
                logger.debug("No asset text unit found for name: {} and asset: {}. Skip delete", name, asset.getPath());
            }
        }
    }

    @Transactional
    void addTextUnit(
            long assetId,
            String name,
            String content,
            String comment,
            PluralForm pluralForm,
            String pluralFormOther,
            boolean doNotTranslate) throws VirtualAssetRequiredException {

        Asset asset = getVirtualAsset(assetId);
        addTextUnit(asset, name, content, comment, pluralForm, pluralFormOther, doNotTranslate);
    }

    void addTextUnit(
            Asset asset,
            String name,
            String content,
            String comment,
            PluralForm pluralForm,
            String pluralFormOther,
            boolean doNotTranslate) throws VirtualAssetRequiredException {

        logger.debug("Add text unit to virtual asset: {}, with name: {}", asset.getPath(), name);

        TextUnitDTO textUnitDTO = new TextUnitDTO();
        textUnitDTO.getAssetTextUnitId();

        Long assetExtractionId = asset.getLastSuccessfulAssetExtraction().getId();
        AssetTextUnit assetTextUnit = assetTextUnitRepository.findByAssetExtractionIdAndName(assetExtractionId, name);
        String newTMTextUnitMD5 = tmService.computeTMTextUnitMD5(name, content, comment);

        if (assetTextUnit != null && newTMTextUnitMD5.equals(assetTextUnit.getMd5())) {
            logger.debug("Asset text unit unchanged, do nothing");
        } else {

            if (assetTextUnit != null) {
                logger.debug("Asset text unit has changed, remove all previous entries");
                assetTextUnitToTMTextUnitRepository.deleteByAssetTextUnitId(assetTextUnit.getId());
                assetTextUnitRepository.delete(assetTextUnit);
            }

            AssetTextUnit previousAssetTextUnit = assetTextUnit;

            logger.debug("Create asset text unit for name: {}, in asset: {}", name, asset.getPath());
            assetTextUnit = assetExtractionService.createAssetTextUnit(
                    asset.getLastSuccessfulAssetExtraction().getId(),
                    name,
                    content,
                    comment,
                    pluralForm,
                    pluralFormOther,
                    doNotTranslate,
                    null);

            logger.debug("Look for an existing TmTextunit");
            TMTextUnit tmTextUnit = tmTextUnitRepository.findFirstByAssetAndMd5(asset, assetTextUnit.getMd5());

            if (tmTextUnit == null) {
                logger.debug("Create TmTextUnit");
                tmTextUnit = tmService.addTMTextUnit(
                        asset.getRepository().getTm().getId(),
                        asset.getId(),
                        name,
                        content,
                        comment,
                        null,
                        pluralForm,
                        pluralFormOther);

                logger.debug("Perform leveraging");
                List<TMTextUnit> tmTextUnits = new ArrayList<>();
                tmTextUnits.add(tmTextUnit);

                if (previousAssetTextUnit != null) {

                    TMTextUnit tmTextUnitForPreviousAssetTextunit = tmTextUnitRepository.findFirstByAssetAndMd5(
                            previousAssetTextUnit.getAssetExtraction().getAsset(),
                            previousAssetTextUnit.getMd5());

                    new LeveragerByTmTextUnit(tmTextUnitForPreviousAssetTextunit.getId()).performLeveragingFor(tmTextUnits, null, null);
                }

                leveragerByContentForSourceLeveraging.performLeveragingFor(tmTextUnits, null, null);
            }

            logger.debug("Map asset text unit to textunit");
            AssetTextUnitToTMTextUnit assetTextUnitToTMTextUnit = new AssetTextUnitToTMTextUnit();
            assetTextUnitToTMTextUnit.setAssetExtraction(assetExtractionRepository.getOne(assetExtractionId));
            assetTextUnitToTMTextUnit.setAssetTextUnit(assetTextUnit);
            assetTextUnitToTMTextUnit.setTmTextUnit(tmTextUnit);
            assetTextUnitToTMTextUnitRepository.save(assetTextUnitToTMTextUnit);

        }
    }

    public TMTextUnitVariant addTextUnitVariant(long assetId, long localeId, String name, String content, String comment)
            throws VirtualAssetRequiredException, VirutalAssetMissingTextUnitException {
        logger.debug("Add text unit variant to virtual assetId: {}, with name: {}", assetId, name);

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setAssetId(assetId);
        textUnitSearcherParameters.setName(name);
        textUnitSearcherParameters.setSearchType(SearchType.EXACT);
        textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
        textUnitSearcherParameters.setLimit(1);

        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        if (textUnitDTOs.isEmpty()) {
            textUnitSearcherParameters.setUsedFilter(UsedFilter.UNUSED);
            textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
        }

        if (textUnitDTOs.isEmpty()) {
            String msg = MessageFormat.format("Missing TmTextUnit for assetId: {0} and name: {1}", assetId, name);
            logger.debug(msg);
            throw new VirutalAssetMissingTextUnitException(msg);
        }

        return tmService.addCurrentTMTextUnitVariant(textUnitDTOs.get(0).getTmTextUnitId(), localeId, content);
    }

    VirtualAssetTextUnit convertAssetTextUnitToVirtualAssetTextUnit(AssetTextUnit assetTextUnit) {

        VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();

        virtualAssetTextUnit.setName(assetTextUnit.getName());
        virtualAssetTextUnit.setContent(assetTextUnit.getContent());
        virtualAssetTextUnit.setComment(assetTextUnit.getComment());
        if (assetTextUnit.getPluralForm() != null) {
            virtualAssetTextUnit.setPluralForm(assetTextUnit.getPluralForm().getName());
        }
        virtualAssetTextUnit.setPluralFormOther(assetTextUnit.getPluralFormOther());
        virtualAssetTextUnit.setDoNotTranslate(assetTextUnit.isDoNotTranslate());

        return virtualAssetTextUnit;
    }

    void normalizeVirtualAssetTextUnit(VirtualAssetTextUnit virtualAssetTextUnit) {
        virtualAssetTextUnit.setName(NormalizationUtils.normalize(virtualAssetTextUnit.getName()));
        virtualAssetTextUnit.setContent(NormalizationUtils.normalize(Strings.nullToEmpty(virtualAssetTextUnit.getContent())));
        virtualAssetTextUnit.setComment(NormalizationUtils.normalize(virtualAssetTextUnit.getComment()));
    }

    Asset getVirtualAsset(long assetId) throws VirtualAssetRequiredException {
        Asset asset = assetRepository.findOne(assetId);

        if (asset == null || asset.getVirtual() == null || !asset.getVirtual()) {
            throw new VirtualAssetRequiredException("Operation requires a Virtual asset");
        }

        return asset;
    }

}
