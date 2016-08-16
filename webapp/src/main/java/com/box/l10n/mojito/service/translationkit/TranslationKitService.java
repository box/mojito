package com.box.l10n.mojito.service.translationkit;

import com.box.l10n.mojito.common.StreamUtil;
import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TranslationKit;
import com.box.l10n.mojito.entity.TranslationKitTextUnit;
import com.box.l10n.mojito.okapi.RawDocument;
import com.box.l10n.mojito.okapi.XLIFFWriter;
import com.box.l10n.mojito.service.drop.DropRepository;
import com.box.l10n.mojito.service.languagedetection.LanguageDetectionResult;
import com.box.l10n.mojito.service.languagedetection.LanguageDetectionService;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import java.nio.charset.StandardCharsets;
import com.google.common.base.Objects;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.steps.common.FilterEventsWriterStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to generate {@link TranslationKit}s
 *
 * @author jaurambault
 */
@Service
public class TranslationKitService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TranslationKitService.class);

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    LocaleService localeService;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    DropRepository dropRepository;

    @Autowired
    TranslationKitRepository translationKitRepository;

    @Autowired
    TranslationKitTextUnitRepository translationKitTextUnitRepository;

    @Autowired
    LanguageDetectionService languageDetectionService;

    @Autowired
    EntityManager entityManager;

    /**
     * Generates and gets a translation kit in XLIFF format for a given
     * {@link TM} and {@link Locale}
     *
     * @param dropId {@link Drop#id}
     * @param tmId {@link TM#id}
     * @param localeId {@link Locale#id}
     * @param type
     * @return the XLIFF content
     */
    @Transactional
    public TranslationKitAsXliff generateTranslationKitAsXLIFF(Long dropId, Long tmId, Long localeId, TranslationKit.Type type) {

        logger.debug("Get translation kit for in tmId: {} and locale: {}", tmId, localeId);
        TranslationKit translationKit = addTranslationKit(dropId, localeId, type);

        logger.trace("Create XLIFFWriter");
        XLIFFWriter xliffWriter = new XLIFFWriter();

        logger.trace("Prepare FilterEventsWriterStep to use an XLIFFWriter with outputstream (allows only one doc to be processed)");
        FilterEventsWriterStep filterEventsWriterStep = new FilterEventsWriterStep(xliffWriter);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        filterEventsWriterStep.setOutputStream(byteArrayOutputStream);
        filterEventsWriterStep.setOutputEncoding(StandardCharsets.UTF_8.toString());

        logger.trace("Prepare the Okapi pipeline");
        IPipelineDriver driver = new PipelineDriver();
        driver.addStep(new RawDocumentToFilterEventsStep(new TranslationKitFilter(translationKit.getId(), type)));
        driver.addStep(new TranslationKitStep(translationKit.getId()));
        driver.addStep(filterEventsWriterStep);

        logger.trace("Add single document with fake output URI to be processed with an outputStream");
        Locale locale = localeService.findById(localeId);
        RawDocument rawDocument = new RawDocument(RawDocument.EMPTY, LocaleId.ENGLISH, LocaleId.fromBCP47(locale.getBcp47Tag()));

        driver.addBatchItem(rawDocument, RawDocument.getFakeOutputURIForStream(), null);

        logger.debug("Start processing batch");
        driver.processBatch();

        logger.trace("Get the output result from the stream");
        TranslationKitAsXliff translationKitAsXliff = new TranslationKitAsXliff();
        translationKitAsXliff.setContent(StreamUtil.getUTF8OutputStreamAsString(byteArrayOutputStream));
        translationKitAsXliff.setTranslationKitId(translationKit.getId());

        return translationKitAsXliff;
    }

    /**
     * Gets the list of {@link TextUnitDTO}s to generate a
     * {@link TranslationKit}
     *
     * @param translationKitId {@link TranslationKit}'s id
     * @param type
     *
     * @return the list of {@link TextUnitDTO}s to generate a
     * {@link TranslationKit}
     */
    public List<TextUnitDTO> getTextUnitDTOsForTranslationKit(Long translationKitId, TranslationKit.Type type) {

        TranslationKit translationKit = translationKitRepository.findOne(translationKitId);

        logger.debug("Get TextUnitDTOs for translation kit: {}", translationKit.getId());
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();

        //TODO(P1) handle "deltas"
        textUnitSearcherParameters.setRepositoryIds(translationKit.getDrop().getRepository().getId());
        textUnitSearcherParameters.setLocaleId(translationKit.getLocale().getId());
        textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
        textUnitSearcherParameters.setStatusFilter(TranslationKit.Type.TRANSLATION.equals(type) ? StatusFilter.FOR_TRANSLATION : StatusFilter.REVIEW_NEEDED);

        return textUnitSearcher.search(textUnitSearcherParameters);
    }

    /**
     * Adds a {@link TranslationKit} to a {@link Drop} for a given
     * {@link Locale}
     *
     * @param dropId {@link Drop#id}
     * @param localeId {@link Locale#id}
     * @param type
     * @return the {@link TranslationKit} that was added
     */
    @Transactional
    public TranslationKit addTranslationKit(Long dropId, Long localeId, TranslationKit.Type type) {

        logger.debug("Add translation kit entities for dropId: {} and localeId: {}", dropId, localeId);

        Drop drop = dropRepository.getOne(dropId);

        TranslationKit translationKit = new TranslationKit();
        translationKit.setDrop(drop);
        translationKit.setLocale(localeService.findById(localeId));
        translationKit.setType(type);

        logger.trace("Save the TranslationKit");
        translationKitRepository.save(translationKit);

        return translationKit;
    }

    /**
     * Update a {@link TranslationKit} with a list of
     * {@link TranslationKitTextUnit#id}s.
     *
     * <p>
     * {@link TranslationKitTextUnit#translationKit} will be set on element base
     * on the given translation kit.
     *
     * @param translationKitId {@link TranslationKit#id}
     * @param translationKitTextUnits List of {@link TranslationKitTextUnit#id}s
     * @param wordCount
     */
    @Transactional
    public void updateTranslationKitWithTmTextUnits(Long translationKitId, List<TranslationKitTextUnit> translationKitTextUnits, Long wordCount) {

        logger.debug("Update translation kit: {} with list of tmTextUnitIds", translationKitId);

        TranslationKit translationKit = translationKitRepository.findOne(translationKitId);
        translationKit.setNumTranslationKitUnits(translationKitTextUnits.size());
        translationKit.setWordCount(wordCount);

        logger.trace("Save the TranslationKit");
        translationKitRepository.save(translationKit);

        for (TranslationKitTextUnit translationKitTextUnit : translationKitTextUnits) {
            logger.trace("Save TranslationKitTextUnit for TranslationKit id: {}, tmTextUnit id: {}", translationKit.getId(), translationKitTextUnit.getTmTextUnit().getId());
            translationKitTextUnit.setTranslationKit(translationKit);
            translationKitTextUnitRepository.save(translationKitTextUnit);
        }
    }

    /**
     * Marks a {@link TranslationKitTextUnit} as imported with provided
     * {@link TMTextUnitVariant} and also perform language detection.
     *
     * @param translationKit
     * @param tmTextUnitVariant
     */
    public void markTranslationKitTextUnitAsImported(TranslationKit translationKit, TMTextUnitVariant tmTextUnitVariant) {

        logger.debug("Mark TranslationKitTextUnit in translationKit: {} imported for TMTextUnit: {}, "
                + "locale: {} with tmTextUnitVariant: {}",
                translationKit,
                tmTextUnitVariant.getTmTextUnit(),
                tmTextUnitVariant.getLocale(),
                tmTextUnitVariant.getId());

        TranslationKitTextUnit translationKitTextUnit = translationKitTextUnitRepository.findByTranslationKitAndTmTextUnitAndTranslationKit_Locale(
                translationKit,
                tmTextUnitVariant.getTmTextUnit(),
                tmTextUnitVariant.getLocale());

        translationKitTextUnit.setImportedTmTextUnitVariant(tmTextUnitVariant);

        //TODO(P1) should be able to read this from the QualityStep information
        translationKitTextUnit.setSourceEqualsTarget(Objects.equal(tmTextUnitVariant.getContentMD5(), tmTextUnitVariant.getTmTextUnit().getContentMd5()));

        String targetLocale = tmTextUnitVariant.getLocale().getBcp47Tag();

        //TODO(P1) Move this to TMTextUnitVariantComment, add annotation
        if (languageDetectionService.isSupportedBcp47Tag(targetLocale)) {
            LanguageDetectionResult languageDetectionResult = languageDetectionService.detect(tmTextUnitVariant.getContent(), targetLocale);
            translationKitTextUnit.setDetectedLanguage(languageDetectionResult.getDetected());
            translationKitTextUnit.setDetectedLanguageProbability(languageDetectionResult.getProbability());
            translationKitTextUnit.setDetectedLanguageExpected(languageDetectionResult.getExpected());

            if (languageDetectionResult.getLangDetectException() != null) {
                translationKitTextUnit.setDetectedLanguageException(languageDetectionResult.getLangDetectException().getMessage());
            }
        } else {
            logger.debug("Language for: {} is not supported by the language detection service, skip detection", targetLocale);
        }

        translationKitTextUnitRepository.save(translationKitTextUnit);
    }

    /**
     * Updates the statistics of a {@link TranslationKit}.
     *
     * @param translationKitId {@link TranslationKit#id}
     * @param notFoundTextUnitIds a list of text unit ids that was in the XLIFF
     * but not in the TM
     */
    @Transactional
    public void updateStatistics(Long translationKitId, Set<String> notFoundTextUnitIds) {

        TranslationKit translationKit = translationKitRepository.findOne(translationKitId);

        translationKit.setNumTranslatedTranslationKitUnits(translationKitTextUnitRepository.countByTranslationKitAndImportedTmTextUnitVariantIsNotNull(translationKit));
        translationKit.setNumSourceEqualsTarget(translationKitTextUnitRepository.countByTranslationKitAndSourceEqualsTargetTrue(translationKit));
        translationKit.setNumBadLanguageDetections(translationKitTextUnitRepository.countByTranslationKitAndDetectedLanguageNotEqualsDetectedLanguageExpected(translationKit));
        translationKit.setNotFoundTextUnitIds(notFoundTextUnitIds);

        translationKitRepository.save(translationKit);
    }

    /**
     * Get exported and current {@link TMTextUnitVariant}s for a
     * {@link TranslationKit} as Map keyed by {@link TMTextUnit#id}
     *
     * @param translationKitId {@link TranslationKit#id}
     * @return the exported and current {@link TMTextUnitVariant}s
     */
    public Map<Long, TranslationKitExportedImportedAndCurrentTUV> getTranslationKitExportedAndCurrentTUVs(Long translationKitId) {

        Map<Long, TranslationKitExportedImportedAndCurrentTUV> map = new HashMap<>();

        logger.debug("Get exported and current tuvs for translationKit id: {}", translationKitId);

        Query createNativeQuery = entityManager.createNamedQuery("TranslationKit.exportedAndCurrentTuvs");
        createNativeQuery.setParameter(1, translationKitId);

        List<TranslationKitExportedImportedAndCurrentTUV> translationKitExportedAndCurrentTUVs = (List<TranslationKitExportedImportedAndCurrentTUV>) createNativeQuery.getResultList();

        for (TranslationKitExportedImportedAndCurrentTUV translationKitExportedAndCurrentTUV : translationKitExportedAndCurrentTUVs) {
            map.put(translationKitExportedAndCurrentTUV.getTmTextUnitId(), translationKitExportedAndCurrentTUV);
        }

        return map;
    }

}
