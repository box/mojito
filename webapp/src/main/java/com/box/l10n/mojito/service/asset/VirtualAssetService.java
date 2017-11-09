package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.AssetTextUnitToTMTextUnit;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.AssetTextUnitToTMTextUnitRepository;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.leveraging.LeveragerByContentForSourceLeveraging;
import com.box.l10n.mojito.service.leveraging.LeveragerByTmTextUnit;
import com.box.l10n.mojito.service.pluralform.PluralFormService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TranslatorWithInheritance;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
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
    PluralFormService pluralFormService;

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
    public List<VirtualAssetTextUnit> getTextUnits(long assetId) throws VirtualAssetRequiredException {

        logger.debug("Get textunit for virtual asset: {}", assetId);

        List<VirtualAssetTextUnit> virtualAssetTextUnits = new ArrayList<>();

        Asset asset = getVirtualAsset(assetId);
        Long lastSuccessfulAssetExtractionId = asset.getLastSuccessfulAssetExtraction().getId();
        List<AssetTextUnit> findByAssetExtractionAssetId = assetTextUnitRepository.findByAssetExtractionIdOrderByNameAsc(lastSuccessfulAssetExtractionId);

        for (AssetTextUnit assetTextUnit : findByAssetExtractionAssetId) {
            VirtualAssetTextUnit textUnitForVirtualAsset = convertAssetTextUnitToVirtualAssetTextUnit(assetTextUnit);
            virtualAssetTextUnits.add(textUnitForVirtualAsset);
        }

        return virtualAssetTextUnits;
    }

    @Transactional
    public List<VirtualAssetTextUnit> getLoalizedTextUnits(
            long assetId,
            long localeId,
            InheritanceMode inheritanceMode) throws VirtualAssetRequiredException {

        logger.debug("Get localized virtual asset: {} for locale: {}", assetId, localeId);
        List<VirtualAssetTextUnit> virtualAssetTextUnits;

        Asset asset = getVirtualAsset(assetId);
        RepositoryLocale repositoryLocale = repositoryLocaleRepository.findByRepositoryIdAndLocaleId(asset.getRepository().getId(), localeId);

        if (repositoryLocale.getParentLocale() == null) {
            logger.debug("Getting text units for the root locale");
            virtualAssetTextUnits = getTextUnits(assetId);
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
                    assetTextUnit.getMd5(),
                    repositoryLocale,
                    inheritanceMode
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

    @Transactional
    public void addTextUnits(List<VirtualAssetTextUnit> virtualAssetTextUnits, long assetId) throws VirtualAssetRequiredException {

        logger.debug("Add text units ({}) to virtual assetId: {}", virtualAssetTextUnits.size(), assetId);

        for (VirtualAssetTextUnit virtualAssetTextUnit : virtualAssetTextUnits) {
            normalizeVirtualAssetTextUnit(virtualAssetTextUnit);
            PluralForm pluralForm = pluralFormService.findByPluralFormString(virtualAssetTextUnit.getPluralForm());

            addTextUnit(
                    assetId,
                    virtualAssetTextUnit.getName(),
                    virtualAssetTextUnit.getContent(),
                    virtualAssetTextUnit.getComment(),
                    pluralForm,
                    virtualAssetTextUnit.getPluralFormOther());
        }
    }

    @Transactional
    public void replaceTextUnits(long assetId, List<VirtualAssetTextUnit> virtualAssetTextUnits) throws VirtualAssetRequiredException {

        logger.debug("Replace text units (new: {}) in virtual assetId: {}", virtualAssetTextUnits.size(), assetId);
        Asset asset = assetRepository.findOne(assetId);

        logger.debug("Create a new AssetExtraction for virtual asset");
        AssetExtraction createAssetExtraction = assetExtractionService.createAssetExtraction(asset, null);
        asset.setLastSuccessfulAssetExtraction(createAssetExtraction);
        assetRepository.save(asset);

        addTextUnits(virtualAssetTextUnits, assetId);
    }

    @Transactional
    public void importLocalizedTextUnits(
            long assetId,
            long localeId,
            List<VirtualAssetTextUnit> textUnitForVirtualAssets) throws VirtualAssetRequiredException {

        logger.debug("Add text unit variants ({}) to virtual assetId: {}", textUnitForVirtualAssets.size(), assetId);

        for (VirtualAssetTextUnit textUnitForVirtualAsset : textUnitForVirtualAssets) {
            normalizeVirtualAssetTextUnit(textUnitForVirtualAsset);
            addTextUnitVariant(
                    assetId,
                    localeId,
                    textUnitForVirtualAsset.getName(),
                    textUnitForVirtualAsset.getContent(),
                    textUnitForVirtualAsset.getComment());
        }
    }

    @Transactional
    public void deleteTextUnit(Long assetId, String name) {

        logger.debug("Try deleting text unit with name: {} from asset id: {}", name, assetId);

        Asset asset = assetRepository.findOne(assetId);
        Long assetExtractionId = asset.getLastSuccessfulAssetExtraction().getId();
        AssetTextUnit assetTextUnit = assetTextUnitRepository.findByAssetExtractionIdAndName(assetExtractionId, name);

        if (assetTextUnit != null) {
            logger.debug("Asset text unit found, perform delete");
            assetTextUnitToTMTextUnitRepository.deleteByAssetTextUnitId(assetTextUnit.getId());
            assetTextUnitRepository.delete(assetTextUnit);
        } else {
            logger.debug("No asset text unit found for name: {} and asset id: {}. Skip delete", name, assetId);
        }
    }

    void addTextUnit(
            long assetId,
            String name,
            String content,
            String comment,
            PluralForm pluralForm,
            String pluralFormOther) throws VirtualAssetRequiredException {

        logger.debug("Add text unit to virtual assetId: {}, with name: {}", assetId, name);
        Asset asset = getVirtualAsset(assetId);

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

            logger.debug("Create asset text unit for name: {}, in asset id: {}", name, assetId);
            assetTextUnit = assetExtractionService.createAssetTextUnit(
                    asset.getLastSuccessfulAssetExtraction().getId(),
                    name,
                    content,
                    comment,
                    pluralForm,
                    pluralFormOther);

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
                    new LeveragerByTmTextUnit(previousAssetTextUnit.getId()).performLeveragingFor(tmTextUnits, null);
                }

                leveragerByContentForSourceLeveraging.performLeveragingFor(tmTextUnits, null);
            }

            logger.debug("Map asset text unit to textunit");
            AssetTextUnitToTMTextUnit assetTextUnitToTMTextUnit = new AssetTextUnitToTMTextUnit();
            assetTextUnitToTMTextUnit.setAssetExtraction(assetExtractionRepository.getOne(assetExtractionId));
            assetTextUnitToTMTextUnit.setAssetTextUnit(assetTextUnit);
            assetTextUnitToTMTextUnit.setTmTextUnit(tmTextUnit);
            assetTextUnitToTMTextUnitRepository.save(assetTextUnitToTMTextUnit);

        }
    }

    TMTextUnitVariant addTextUnitVariant(long assetId, long localeId, String name, String content, String comment) throws VirtualAssetRequiredException {
        logger.debug("Add text unit variant to virtual assetId: {}, with name: {}", assetId, name);
        TMTextUnit findFirstByAssetAndName = tmTextUnitRepository.findFirstByAssetAndName(assetRepository.getOne(assetId), name);
        return tmService.addCurrentTMTextUnitVariant(findFirstByAssetAndName.getId(), localeId, content);
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
