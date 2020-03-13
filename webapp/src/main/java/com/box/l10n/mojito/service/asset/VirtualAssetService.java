package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetExtraction_;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.AssetTextUnit_;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PluralForm_;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.AssetTextUnitToTMTextUnitRepository;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitDTO;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.leveraging.LeveragerByContentForSourceLeveraging;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pluralform.PluralFormService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TranslatorWithInheritance;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.SearchType;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.google.common.base.Strings;
import com.ibm.icu.text.MessageFormat;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.box.l10n.mojito.entity.TMTextUnitVariant.Status.APPROVED;
import static org.slf4j.LoggerFactory.getLogger;

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
    RepositoryService repositoryService;

    @Autowired
    LocaleService localeService;

    @Autowired
    QuartzPollableTaskScheduler quartzPollableTaskScheduler;

    @Autowired
    EntityManager em;

    @Autowired
    PluralFormService pluralFormService;

    @Transactional
    public VirtualAsset createOrUpdateVirtualAsset(VirtualAsset virtualAsset) throws VirtualAssetBadRequestException {

        if (virtualAsset.getRepositoryId() == null) {
            throw new VirtualAssetBadRequestException("A repository id must be provided");
        }

        logger.debug("Create or update virtual asset with path: {}", virtualAsset.getPath());
        Asset asset = assetRepository.findByPathAndRepositoryId(virtualAsset.getPath(), virtualAsset.getRepositoryId());

        if (asset != null && !asset.getVirtual()) {
            throw new VirtualAssetRequiredException("Standard asset can't be modify");
        } else if (asset != null) {
            logger.debug("Virtual asset exists, update it");
            if (virtualAsset.getDeleted() == null) {
                throw new VirtualAssetRequiredException("Missing deleted attribute for update");
            }
            asset.setDeleted(virtualAsset.getDeleted());
            asset = assetRepository.save(asset);
        } else {
            logger.debug("No existing virtual asset, create one");

            Repository repository = repositoryRepository.findById(virtualAsset.getRepositoryId()).orElse(null);

            if (repository == null) {
                throw new VirtualAssetBadRequestException("A valid repository must be provided when creating an asset");
            }

            asset = new Asset();
            asset.setRepository(repository);
            asset.setPath(virtualAsset.getPath());
            asset.setVirtual(Boolean.TRUE);

            logger.debug("Create a default AssetExtraction for virtual asset");
            AssetExtraction createAssetExtraction = assetExtractionService.createAssetExtraction(asset, null);
            asset.setLastSuccessfulAssetExtraction(createAssetExtraction);

            asset = assetRepository.save(asset);
        }

        virtualAsset.setId(asset.getId());
        return virtualAsset;
    }

    public List<VirtualAssetTextUnit> getTextUnits(long assetId, Boolean doNotTranslateFilter) throws VirtualAssetRequiredException {

        logger.debug("Get textunit for virtual asset: {}", assetId);
        Asset asset = getVirtualAsset(assetId);
        Long lastSuccessfulAssetExtractionId = asset.getLastSuccessfulAssetExtraction().getId();

        List<AssetTextUnitDTO> findByAssetExtractionAssetId = findByAssetExtractionIdAndDoNotTranslateFilter(lastSuccessfulAssetExtractionId, doNotTranslateFilter);

        List<VirtualAssetTextUnit> virtualAssetTextUnits = findByAssetExtractionAssetId.stream()
                .map(this::convertAssetTextUnitDTOToVirtualAssetTextUnit)
                .collect(Collectors.toList());

        return virtualAssetTextUnits;
    }

    @Transactional
    List<AssetTextUnitDTO> findByAssetExtractionIdAndDoNotTranslateFilter(Long assetExtractionId, Boolean doNotTranslateFilter) {

        Map<ParameterExpression, Object> parameters = new LinkedHashMap<>();

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<AssetTextUnitDTO> query = criteriaBuilder.createQuery(AssetTextUnitDTO.class);

        Root<AssetTextUnit> assetTextUnitRoot = query.from(AssetTextUnit.class);

        Predicate conjunction = criteriaBuilder.conjunction();

        ParameterExpression<Long> assetExtractionIdParameter = criteriaBuilder.parameter(Long.class);
        parameters.put(assetExtractionIdParameter, assetExtractionId);
        conjunction.getExpressions().add(criteriaBuilder.equal(assetTextUnitRoot.get(AssetTextUnit_.assetExtraction).get(AssetExtraction_.id), assetExtractionIdParameter));

        if (doNotTranslateFilter != null) {
            ParameterExpression<String> doNotTranslateFilterParameter = criteriaBuilder.parameter(String.class);
            parameters.put(doNotTranslateFilterParameter, doNotTranslateFilter);
            conjunction.getExpressions().add(criteriaBuilder.equal(assetTextUnitRoot.get(AssetTextUnit_.doNotTranslate), doNotTranslateFilterParameter));
        }

        query.select(criteriaBuilder.construct(AssetTextUnitDTO.class,
                assetTextUnitRoot.get(AssetTextUnit_.name),
                assetTextUnitRoot.get(AssetTextUnit_.content),
                assetTextUnitRoot.get(AssetTextUnit_.comment),
                assetTextUnitRoot.get(AssetTextUnit_.pluralForm).get(PluralForm_.id),
                assetTextUnitRoot.get(AssetTextUnit_.pluralFormOther),
                assetTextUnitRoot.get(AssetTextUnit_.md5),
                assetTextUnitRoot.get(AssetTextUnit_.doNotTranslate)
        ));

        query.where(conjunction);
        query.orderBy(criteriaBuilder.asc(assetTextUnitRoot.get(AssetTextUnit_.name)));

        TypedQuery<AssetTextUnitDTO> typedQuery = em.createQuery(query);

        parameters.forEach((parameterExpression, o) -> {
            typedQuery.setParameter(parameterExpression, o);
        });

        List<AssetTextUnitDTO> resultList = typedQuery.getResultList();
        return resultList;
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
            virtualAssetTextUnits = getLocalizedTextUnitsForTargetLocale(asset, repositoryLocale, inheritanceMode);
        }

        return virtualAssetTextUnits;
    }

    List<VirtualAssetTextUnit> getLocalizedTextUnitsForTargetLocale(Asset asset, RepositoryLocale repositoryLocale, InheritanceMode inheritanceMode) {
        logger.debug("Get localized virtual asset for target locale");
        List<VirtualAssetTextUnit> virtualAssetTextUnits = new ArrayList<>();

        Long lastSuccessfulAssetExtractionId = asset.getLastSuccessfulAssetExtraction().getId();

        List<AssetTextUnitDTO> findByAssetExtractionAssetId = findByAssetExtractionIdAndDoNotTranslateFilter(lastSuccessfulAssetExtractionId, null);

        TranslatorWithInheritance translatorWithInheritance = new TranslatorWithInheritance(asset, repositoryLocale, inheritanceMode);

        for (AssetTextUnitDTO assetTextUnit : findByAssetExtractionAssetId) {

            String translation = translatorWithInheritance.getTranslation(
                    assetTextUnit.getContent(),
                    assetTextUnit.getMd5()
            );

            if (translation == null && InheritanceMode.REMOVE_UNTRANSLATED.equals(inheritanceMode)) {
                logger.debug("Remove untranslated text unit");
            } else {
                logger.debug("Set translation for text unit with name: {}, translation: {}", assetTextUnit.getName(), translation);

                VirtualAssetTextUnit virtualAssetTextUnit = convertAssetTextUnitDTOToVirtualAssetTextUnit(assetTextUnit);
                virtualAssetTextUnit.setContent(translation);
                virtualAssetTextUnits.add(virtualAssetTextUnit);
            }
        }

        return virtualAssetTextUnits;
    }

    public PollableFuture<Void> addTextUnits(long assetId, List<VirtualAssetTextUnit> virtualAssetTextUnits) throws VirtualAssetRequiredException {
        VirtualTextUnitBatchUpdateJobInput importVirtualAssetJobInput = new VirtualTextUnitBatchUpdateJobInput();
        importVirtualAssetJobInput.setAssetId(assetId);
        importVirtualAssetJobInput.setVirtualAssetTextUnits(virtualAssetTextUnits);
        importVirtualAssetJobInput.setReplace(false);
        return quartzPollableTaskScheduler.scheduleJob(VirtualTextUnitBatchUpdateJob.class, importVirtualAssetJobInput);
    }

    public PollableFuture<Void> replaceTextUnits(long assetId, List<VirtualAssetTextUnit> virtualAssetTextUnits) throws VirtualAssetRequiredException {
        VirtualTextUnitBatchUpdateJobInput importVirtualAssetJobInput = new VirtualTextUnitBatchUpdateJobInput();
        importVirtualAssetJobInput.setAssetId(assetId);
        importVirtualAssetJobInput.setVirtualAssetTextUnits(virtualAssetTextUnits);
        importVirtualAssetJobInput.setReplace(true);
        return quartzPollableTaskScheduler.scheduleJob(VirtualTextUnitBatchUpdateJob.class, importVirtualAssetJobInput);
    }

    public PollableFuture<Void> importLocalizedTextUnits(
            long assetId,
            long localeId,
            List<VirtualAssetTextUnit> textUnitForVirtualAssets)
            throws VirtualAssetRequiredException, VirutalAssetMissingTextUnitException {

        logger.debug("Add text unit variants ({}) to virtual assetId: {}", textUnitForVirtualAssets.size(), assetId);

        List<TextUnitDTO> textUnitDTOs = new ArrayList<>();

        Asset asset = assetRepository.findById(assetId).orElse(null);
        Locale locale = localeService.findById(localeId);

        for (VirtualAssetTextUnit textUnitForVirtualAsset : textUnitForVirtualAssets) {
            TextUnitDTO textUnitDTO = new TextUnitDTO();
            textUnitDTO.setRepositoryName(asset.getRepository().getName());
            textUnitDTO.setAssetPath(asset.getPath());
            textUnitDTO.setTargetLocale(locale.getBcp47Tag());
            textUnitDTO.setName(textUnitForVirtualAsset.getName());
            textUnitDTO.setTarget(textUnitForVirtualAsset.getContent());
            textUnitDTO.setComment(textUnitForVirtualAsset.getComment());
            textUnitDTO.setIncludedInLocalizedFile(true);
            textUnitDTO.setStatus(APPROVED);
            textUnitDTOs.add(textUnitDTO);
        }

        return textUnitBatchImporterService.asyncImportTextUnits(textUnitDTOs, false, false);
    }

    @Transactional
    public void deleteTextUnit(Long assetId, String name) {
        Asset asset = assetRepository.findById(assetId).orElse(null);
        deleteTextUnits(asset, Arrays.asList(name));
    }

    public void deleteTextUnits(Asset asset, List<String> names) {

        Long assetExtractionId = asset.getLastSuccessfulAssetExtraction().getId();

        for (String name : names) {
            logger.debug("Try deleting text units with name: {} from asset: {}", name, asset.getPath());

            List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtractionIdAndName(assetExtractionId, name);

            for (AssetTextUnit assetTextUnit : assetTextUnits) {
                logger.debug("Asset text unit found for name: {} and asset: {}, perform delete", name, asset.getPath());
                assetTextUnitToTMTextUnitRepository.deleteByAssetTextUnitId(assetTextUnit.getId());
                assetTextUnitRepository.delete(assetTextUnit);
            }
        }
    }

    @Transactional
    void addTextUnit(
            long assetId,
            String name,
            String content,
            String comment,
            String pluralForm,
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
            String pluralForm,
            String pluralFormOther,
            boolean doNotTranslate) throws VirtualAssetRequiredException {

        VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
        virtualAssetTextUnit.setName(name);
        virtualAssetTextUnit.setContent(content);
        virtualAssetTextUnit.setComment(comment);
        virtualAssetTextUnit.setPluralForm(pluralForm);
        virtualAssetTextUnit.setPluralFormOther(pluralFormOther);
        virtualAssetTextUnit.setDoNotTranslate(doNotTranslate);

        List<VirtualAssetTextUnit> virtualAssetTextUnits = new ArrayList<>();
        virtualAssetTextUnits.add(virtualAssetTextUnit);

        virtualTextUnitBatchUpdaterService.updateTextUnits(asset, virtualAssetTextUnits, false);
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

    VirtualAssetTextUnit convertAssetTextUnitDTOToVirtualAssetTextUnit(AssetTextUnitDTO assetTextUnitDTO) {

        VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();

        virtualAssetTextUnit.setName(assetTextUnitDTO.getName());
        virtualAssetTextUnit.setContent(assetTextUnitDTO.getContent());
        virtualAssetTextUnit.setComment(assetTextUnitDTO.getComment());
        if (assetTextUnitDTO.getPluralFormId() != null) {
            virtualAssetTextUnit.setPluralForm(pluralFormService.findById(assetTextUnitDTO.getPluralFormId()).getName());
        }
        virtualAssetTextUnit.setPluralFormOther(assetTextUnitDTO.getPluralFormOther());
        virtualAssetTextUnit.setDoNotTranslate(assetTextUnitDTO.isDoNotTranslate());

        return virtualAssetTextUnit;
    }

    void normalizeVirtualAssetTextUnit(VirtualAssetTextUnit virtualAssetTextUnit) {
        virtualAssetTextUnit.setName(NormalizationUtils.normalize(virtualAssetTextUnit.getName()));
        virtualAssetTextUnit.setContent(NormalizationUtils.normalize(Strings.nullToEmpty(virtualAssetTextUnit.getContent())));
        virtualAssetTextUnit.setComment(NormalizationUtils.normalize(virtualAssetTextUnit.getComment()));
    }

    Asset getVirtualAsset(long assetId) throws VirtualAssetRequiredException {
        Asset asset = assetRepository.findById(assetId).orElse(null);

        if (asset == null || asset.getVirtual() == null || !asset.getVirtual()) {
            throw new VirtualAssetRequiredException("Operation requires a Virtual asset");
        }

        return asset;
    }
}
