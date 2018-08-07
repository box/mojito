package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.common.StreamUtil;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMXliff;
import com.box.l10n.mojito.okapi.AbstractImportTranslationsStep;
import com.box.l10n.mojito.okapi.CheckForDoNotTranslateStep;
import com.box.l10n.mojito.okapi.CopyFormsOnImport;
import com.box.l10n.mojito.okapi.FilterEventsToInMemoryRawDocumentStep;
import com.box.l10n.mojito.okapi.ImportTranslationsByIdStep;
import com.box.l10n.mojito.okapi.ImportTranslationsByMd5Step;
import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep;
import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget;
import com.box.l10n.mojito.okapi.ImportTranslationsStepAnnotation;
import com.box.l10n.mojito.okapi.ImportTranslationsWithTranslationKitStep;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.okapi.PseudoLocalizeStep;
import com.box.l10n.mojito.okapi.RawDocument;
import com.box.l10n.mojito.okapi.Status;
import com.box.l10n.mojito.okapi.TranslateStep;
import com.box.l10n.mojito.okapi.XLIFFWriter;
import com.box.l10n.mojito.okapi.qualitycheck.Parameters;
import com.box.l10n.mojito.okapi.qualitycheck.QualityCheckStep;
import com.box.l10n.mojito.rest.asset.FilterConfigIdOverride;
import com.box.l10n.mojito.security.AuditorAwareImpl;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.WordCountService;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetExtraction.extractor.AssetExtractor;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckStep;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.InjectCurrentTask;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.xliff.XliffUtils;
import com.google.common.base.Preconditions;
import com.ibm.icu.text.MessageFormat;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.persistence.EntityManager;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import net.sf.okapi.steps.common.FilterEventsWriterStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to manage {@link TM}s (translation memories).
 * <p/>
 * Allows to add {@link TMTextUnit}s (entities to be translated) and
 * {@link TMTextUnitVariant} (actual translations). The current translations are
 * marked using {@link TMTextUnitCurrentVariant}.
 *
 * @author jaurambault
 */
@Service
public class TMService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TMService.class);

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    @Autowired
    LocaleService localeService;

    @Autowired
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    AssetExtractor assetExtractor;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    XliffUtils xliffUtils;

    @Autowired
    WordCountService wordCountService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    TMXliffRepository tmXliffRepository;

    @Autowired
    AuditorAwareImpl auditorAwareImpl;

    /**
     * Adds a {@link TMTextUnit} in a {@link TM}.
     *
     * @param tmId the {@link TM} id (must be valid)
     * @param assetId the {@link Asset} id (must be valid)
     * @param name the text unit name
     * @param content the text unit content
     * @param comment the text unit comment, can be {@code null}
     * @return the create {@link TMTextUnit}
     * @throws DataIntegrityViolationException If trying to create a
     * {@link TMTextUnit} with same logical key as an existing one or TM id
     * invalid
     */
    @Transactional
    public TMTextUnit addTMTextUnit(Long tmId, Long assetId, String name, String content, String comment) {
        return addTMTextUnit(tmId, assetId, name, content, comment, null, null, null);
    }

    /**
     * Adds a {@link TMTextUnit} in a {@link TM}.
     *
     * @param tmId the {@link TM} id (must be valid)
     * @param assetId the {@link Asset} id (must be valid)
     * @param name the text unit name
     * @param content the text unit content
     * @param comment the text unit comment, can be {@code null}
     * @param createdDate to specify a creation date (can be used to re-import
     * old TM), can be {@code null}
     * @return the create {@link TMTextUnit}
     * @throws DataIntegrityViolationException If trying to create a
     * {@link TMTextUnit} with same logical key as an existing one or TM id
     * invalid
     */
    @Transactional
    public TMTextUnit addTMTextUnit(
            Long tmId,
            Long assetId,
            String name,
            String content,
            String comment,
            DateTime createdDate,
            PluralForm puralForm,
            String pluralFormOther) {

        logger.debug("Add TMTextUnit in tmId: {} with name: {}, content: {}, comment: {}", tmId, name, content, comment);
        TMTextUnit tmTextUnit = new TMTextUnit();

        tmTextUnit.setTm(entityManager.getReference(TM.class, tmId));
        tmTextUnit.setAsset(entityManager.getReference(Asset.class, assetId));
        tmTextUnit.setName(name);
        tmTextUnit.setContent(content);
        tmTextUnit.setComment(comment);
        tmTextUnit.setMd5(computeTMTextUnitMD5(name, content, comment));
        //TODO(P1) Compute word count for english, root locale is hard coded is {@link RepositoryService}
        tmTextUnit.setWordCount(wordCountService.getEnglishWordCount(content));
        tmTextUnit.setContentMd5(DigestUtils.md5Hex(content));
        tmTextUnit.setCreatedDate(createdDate);
        tmTextUnit.setPluralForm(puralForm);
        tmTextUnit.setPluralFormOther(pluralFormOther);
        tmTextUnit.setCreatedByUser(auditorAwareImpl.getCurrentAuditor());

        tmTextUnit = tmTextUnitRepository.save(tmTextUnit);
        logger.trace("TMTextUnit saved");

        logger.debug("Add a current TMTextUnitVariant for the source text ie. the default locale");
        TMTextUnitVariant addTMTextUnitVariant = addTMTextUnitVariant(tmTextUnit.getId(), localeService.getDefaultLocaleId(), content, comment, TMTextUnitVariant.Status.APPROVED, true, createdDate);
        makeTMTextUnitVariantCurrent(tmId, tmTextUnit.getId(), localeService.getDefaultLocaleId(), addTMTextUnitVariant.getId());

        return tmTextUnit;
    }

    /**
     * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a
     * locale other than the default locale.
     * <p/>
     * Also checks for an existing {@link TMTextUnitCurrentVariant} and if it
     * references a {@link TMTextUnitVariant} that has same content, the
     * {@link TMTextUnitVariant} is returned and no entities are created.
     *
     * @param tmTextUnitId the text unit that will contains the translation
     * @param localeId locale id of the translation (default locale not
     * accepted)
     * @param content the translation content
     * @return the created {@link TMTextUnitVariant} or an existing one with
     * same content
     * @throws DataIntegrityViolationException If tmTextUnitId or localeId are
     * invalid
     */
    public TMTextUnitVariant addCurrentTMTextUnitVariant(Long tmTextUnitId, Long localeId, String content) {
        return addTMTextUnitCurrentVariant(tmTextUnitId, localeId, content, null).getTmTextUnitVariant();
    }

    /**
     * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a
     * locale other than the default locale.
     * <p/>
     * Also checks for an existing {@link TMTextUnitCurrentVariant} and if it
     * references a {@link TMTextUnitVariant} that has same content, the
     * {@link TMTextUnitVariant} is returned and no entities are created.
     *
     * @param tmTextUnitId the text unit that will contains the translation
     * @param localeId locale id of the translation (default locale not
     * accepted)
     * @param content the translation content
     * @param status the translation status
     * @param includedInLocalizedFile indicate if the translation should be
     * included or not in the localized files
     * @return the created {@link TMTextUnitVariant} or an existing one with
     * same content
     * @throws DataIntegrityViolationException If tmTextUnitId or localeId are
     * invalid
     */
    public TMTextUnitVariant addCurrentTMTextUnitVariant(Long tmTextUnitId, Long localeId, String content, TMTextUnitVariant.Status status, boolean includedInLocalizedFile) {
        return addCurrentTMTextUnitVariant(tmTextUnitId, localeId, content, status, includedInLocalizedFile, null);
    }

    /**
     * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a
     * locale other than the default locale.
     * <p/>
     * Also checks for an existing {@link TMTextUnitCurrentVariant} and if it
     * references a {@link TMTextUnitVariant} that has same content, the
     * {@link TMTextUnitVariant} is returned and no entities are created.
     *
     * @param tmTextUnitId the text unit that will contains the translation
     * @param localeId locale id of the translation (default locale not
     * accepted)
     * @param content the translation content
     * @param status the translation status
     * @param includedInLocalizedFile indicate if the translation should be
     * included or not in the localized files
     * @param createdDate to specify a creation date (can be used to re-import
     * old TM), can be {@code null}
     * @return the created {@link TMTextUnitVariant} or an existing one with
     * same content
     * @throws DataIntegrityViolationException If tmTextUnitId or localeId are
     * invalid
     */
    public TMTextUnitVariant addCurrentTMTextUnitVariant(Long tmTextUnitId, Long localeId, String content, TMTextUnitVariant.Status status, boolean includedInLocalizedFile, DateTime createdDate) {
        return addTMTextUnitCurrentVariant(tmTextUnitId, localeId, content, null, status, includedInLocalizedFile, createdDate).getTmTextUnitVariant();
    }

    /**
     * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a
     * locale other than the default locale.
     * <p/>
     * Also checks for an existing {@link TMTextUnitCurrentVariant} and if it
     * references a {@link TMTextUnitVariant} that has same content, the
     * {@link TMTextUnitVariant} is returned and no entities are created.
     *
     * @param tmTextUnitId the text unit that will contains the translation
     * @param localeId locale id of the translation (default locale not
     * accepted)
     * @param content the translation content
     * @param comment the translation comment, can be {@code null}
     * @return the {@link TMTextUnitCurrentVariant} that holds the created
     * {@link TMTextUnitVariant} or an existing one with same content
     * @throws DataIntegrityViolationException If tmTextUnitId or localeId are
     * invalid
     */
    public TMTextUnitCurrentVariant addTMTextUnitCurrentVariant(Long tmTextUnitId, Long localeId, String content, String comment) {
        return addTMTextUnitCurrentVariant(tmTextUnitId, localeId, content, comment, TMTextUnitVariant.Status.APPROVED);
    }

    /**
     * @see TMService#addTMTextUnitCurrentVariant(Long, Long, String, String,
     * boolean, boolean)
     *
     * @param tmTextUnitId the text unit that will contains the translation
     * @param localeId locale id of the translation (default locale not
     * accepted)
     * @param content the translation content
     * @param comment the translation comment, can be {@code null}
     * @param status the translation status
     * @return the {@link TMTextUnitCurrentVariant} that holds the created
     * {@link TMTextUnitVariant} or an existing one with same content
     * @throws DataIntegrityViolationException If tmTextUnitId or localeId are
     * invalid
     */
    public TMTextUnitCurrentVariant addTMTextUnitCurrentVariant(Long tmTextUnitId, Long localeId, String content, String comment, TMTextUnitVariant.Status status) {
        return addTMTextUnitCurrentVariant(tmTextUnitId, localeId, content, comment, status, true);
    }

    /**
     * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a
     * locale other than the default locale.
     * <p/>
     * Also checks for an existing {@link TMTextUnitCurrentVariant} and if it
     * references a {@link TMTextUnitVariant} that has same content, the
     * {@link TMTextUnitVariant} is returned and no entities are created.
     *
     * @param tmTextUnitId the text unit that will contains the translation
     * @param localeId locale id of the translation (default locale not
     * accepted)
     * @param content the translation content
     * @param comment the translation comment, can be {@code null}
     * @param status the translation status
     * @param includedInLocalizedFile indicate if the translation should be
     * included or not in the localized files
     * @return the {@link TMTextUnitCurrentVariant} that holds the created
     * {@link TMTextUnitVariant} or an existing one with same content
     * @throws DataIntegrityViolationException If tmTextUnitId or localeId are
     * invalid
     */
    public TMTextUnitCurrentVariant addTMTextUnitCurrentVariant(
            Long tmTextUnitId,
            Long localeId,
            String content,
            String comment,
            TMTextUnitVariant.Status status,
            boolean includedInLocalizedFile) {
        return addTMTextUnitCurrentVariant(tmTextUnitId, localeId, content, comment, status, includedInLocalizedFile, null);
    }

    /**
     * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a
     * locale other than the default locale.
     * <p/>
     * Also checks for an existing {@link TMTextUnitCurrentVariant} and if it
     * references a {@link TMTextUnitVariant} that has same content, the
     * {@link TMTextUnitVariant} is returned and no entities are created.
     *
     * @param tmTextUnitId the text unit that will contains the translation
     * @param localeId locale id of the translation (default locale not
     * accepted)
     * @param content the translation content
     * @param comment the translation comment, can be {@code null}
     * @param status the translation status
     * @param includedInLocalizedFile indicate if the translation should be
     * included or not in the localized files
     * @param createdDate to specify a creation date (can be used to re-import
     * old TM), can be {@code null}
     * @return the {@link TMTextUnitCurrentVariant} that holds the created
     * {@link TMTextUnitVariant} or an existing one with same content
     * @throws DataIntegrityViolationException If tmTextUnitId or localeId are
     * invalid
     */
    public TMTextUnitCurrentVariant addTMTextUnitCurrentVariant(
            Long tmTextUnitId,
            Long localeId,
            String content,
            String comment,
            TMTextUnitVariant.Status status,
            boolean includedInLocalizedFile,
            DateTime createdDate) {

        return addTMTextUnitCurrentVariantWithResult(tmTextUnitId, localeId, content, comment, status, includedInLocalizedFile, createdDate).getTmTextUnitCurrentVariant();
    }

    /**
     * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a
     * locale other than the default locale.
     * <p/>
     * Also checks for an existing {@link TMTextUnitCurrentVariant} and if it
     * references a {@link TMTextUnitVariant} that has same content, the
     * {@link TMTextUnitVariant} is returned and no entities are created.
     *
     * @param tmTextUnitId the text unit that will contains the translation
     * @param localeId locale id of the translation (default locale not
     * accepted)
     * @param content the translation content
     * @param comment the translation comment, can be {@code null}
     * @param status the translation status
     * @param includedInLocalizedFile indicate if the translation should be
     * included or not in the localized files
     * @param createdDate to specify a creation date (can be used to re-import
     * old TM), can be {@code null}
     * @return the result that contains the {@link TMTextUnitCurrentVariant} and
     * indicates if it was updated or not. The {@link TMTextUnitCurrentVariant}
     * holds the created {@link TMTextUnitVariant} or an existing one with same
     * content
     * @throws DataIntegrityViolationException If tmTextUnitId or localeId are
     * invalid
     */
    public AddTMTextUnitCurrentVariantResult addTMTextUnitCurrentVariantWithResult(
            Long tmTextUnitId,
            Long localeId,
            String content,
            String comment,
            TMTextUnitVariant.Status status,
            boolean includedInLocalizedFile,
            DateTime createdDate) {

        logger.debug("Check if there is a current TMTextUnitVariant");
        TMTextUnitCurrentVariant currentTmTextUnitCurrentVariant = tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(localeId, tmTextUnitId);

        TMTextUnit tmTextUnit = tmTextUnitRepository.findOne(tmTextUnitId);

        if (tmTextUnit == null) {
            String msg = MessageFormat.format("Unable to find the TMTextUnit with ID: {0}. The TMTextUnitVariant and "
                    + "TMTextUnitCurrentVariant will not be created.", tmTextUnitId);
            throw new RuntimeException(msg);
        }

        return addTMTextUnitCurrentVariantWithResult(currentTmTextUnitCurrentVariant,
                tmTextUnit.getTm().getId(),
                tmTextUnitId,
                localeId,
                content,
                comment,
                status,
                includedInLocalizedFile,
                createdDate);
    }

    /**
     * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a
     * locale other than the default locale.
     * <p/>
     * Requires the {@link TMTextUnitCurrentVariant} and TM id for optimization
     * purpose.
     *
     * @param tmTextUnitCurrentVariant current variant or null is there is none
     * @param tmId the {@link TM} id in which the translation is added
     * @param tmTextUnitId the text unit that will contains the translation
     * @param localeId locale id of the translation (default locale not
     * accepted)
     * @param content the translation content
     * @param comment the translation comment, can be {@code null}
     * @param status the translation status
     * @param includedInLocalizedFile indicate if the translation should be
     * included or not in the localized files
     * @param createdDate to specify a creation date (can be used to re-import
     * old TM), can be {@code null}
     * @return the result that contains the {@link TMTextUnitCurrentVariant} and
     * indicates if it was updated or not. The {@link TMTextUnitCurrentVariant}
     * holds the created {@link TMTextUnitVariant} or an existing one with same
     * content
     * @throws DataIntegrityViolationException If tmTextUnitId or localeId are
     * invalid
     */
    public AddTMTextUnitCurrentVariantResult addTMTextUnitCurrentVariantWithResult(
            TMTextUnitCurrentVariant tmTextUnitCurrentVariant,
            Long tmId,
            Long tmTextUnitId,
            Long localeId,
            String content,
            String comment,
            TMTextUnitVariant.Status status,
            boolean includedInLocalizedFile,
            DateTime createdDate) {

        if (localeService.getDefaultLocaleId().equals(localeId)) {
            throw new RuntimeException("Cannot add text unit variant for the default locale");
        }

        boolean noUpdate = false;

        TMTextUnitVariant tmTextUnitVariant;

        if (tmTextUnitCurrentVariant == null) {
            logger.debug("There is no currrent text unit variant, add entities");
            tmTextUnitVariant = addTMTextUnitVariant(tmTextUnitId, localeId, content, comment, status, includedInLocalizedFile, createdDate);
            tmTextUnitCurrentVariant = makeTMTextUnitVariantCurrent(tmId, tmTextUnitId, localeId, tmTextUnitVariant.getId());

            logger.trace("Put the actual tmTextUnitVariant instead of the proxy");
            tmTextUnitCurrentVariant.setTmTextUnitVariant(tmTextUnitVariant);
        } else {

            logger.debug("There is a current text unit variant, check if an update is needed");
            TMTextUnitVariant currentTmTextUnitVariant = tmTextUnitCurrentVariant.getTmTextUnitVariant();

            boolean updateNeeded = isUpdateNeededForTmTextUnitVariant(
                    currentTmTextUnitVariant.getStatus(),
                    currentTmTextUnitVariant.getContentMD5(),
                    currentTmTextUnitVariant.isIncludedInLocalizedFile(),
                    currentTmTextUnitVariant.getComment(),
                    status,
                    DigestUtils.md5Hex(content),
                    includedInLocalizedFile,
                    comment);

            if (updateNeeded) {
                logger.debug("The current text unit variant has different content, comment or needs review. Add entities");
                tmTextUnitVariant = addTMTextUnitVariant(tmTextUnitId, localeId, content, comment, status, includedInLocalizedFile, createdDate);

                logger.debug("Updating the current TextUnitVariant with id: {} current for locale: {}", tmTextUnitVariant.getId(), localeId);
                tmTextUnitCurrentVariant.setTmTextUnitVariant(tmTextUnitVariant);
                tmTextUnitCurrentVariantRepository.save(tmTextUnitCurrentVariant);
            } else {
                logger.debug("The current text unit variant has same content, comment and review status, don't add entities and return it instead");
                noUpdate = true;
            }
        }

        return new AddTMTextUnitCurrentVariantResult(!noUpdate, tmTextUnitCurrentVariant);
    }

    /**
     * Indicates if a {@link TMTextUnitVariant} should be updated by looking at
     * new/old content, status, comments, etc
     *
     * @param currentStatus
     * @param currentContentMd5
     * @param currentIncludedInLocalizedFile
     * @param currentComment
     * @param newStatus
     * @param newContentMd5
     * @param newIncludedInLocalizedFile
     * @param newComment
     * @return
     */
    public boolean isUpdateNeededForTmTextUnitVariant(
            TMTextUnitVariant.Status currentStatus,
            String currentContentMd5,
            boolean currentIncludedInLocalizedFile,
            String currentComment,
            TMTextUnitVariant.Status newStatus,
            String newContentMd5,
            boolean newIncludedInLocalizedFile,
            String newComment) {

        return !(currentContentMd5.equals(newContentMd5)
                && currentStatus.equals(newStatus)
                && currentIncludedInLocalizedFile == newIncludedInLocalizedFile
                && Objects.equals(currentComment, newComment));
    }

    /**
     * Adds a {@link TMTextUnitVariant} in a {@link TMTextUnit}.
     * <p/>
     * No checks are performed on the locale or for duplicated content. If this
     * is a requirement use {@link #addCurrentTMTextUnitVariant(java.lang.Long, java.lang.Long, java.lang.String)
     *
     * @param tmTextUnitId the text unit that will contain the translation
     * @param localeId locale id of the translation
     * @param content the translation content
     * @param comment comment for the translation, can be {@code null}
     * @param status the translation status
     * @param includedInLocalizedFile indicate if the translation should be
     * included or not in the localized files
     * @return the created {@link TMTextUnitVariant}
     * @throws DataIntegrityViolationException If tmTextUnitId or localeId are
     * invalid
     */
    protected TMTextUnitVariant addTMTextUnitVariant(
            Long tmTextUnitId,
            Long localeId,
            String content,
            String comment,
            TMTextUnitVariant.Status status,
            boolean includedInLocalizedFile) {

        return addTMTextUnitVariant(tmTextUnitId, localeId, content, comment, status, includedInLocalizedFile, null);
    }

    /**
     * Adds a {@link TMTextUnitVariant} in a {@link TMTextUnit}.
     * <p/>
     * No checks are performed on the locale or for duplicated content. If this
     * is a requirement use {@link #addCurrentTMTextUnitVariant(java.lang.Long, java.lang.Long, java.lang.String)
     *
     * @param tmTextUnitId the text unit that will contain the translation
     * @param localeId locale id of the translation
     * @param content the translation content
     * @param comment comment for the translation, can be {@code null}
     * @param status the translation status
     * @param includedInLocalizedFile indicate if the translation should be
     * included or not in the localized files
     * @param createdDate to specify a creation date (can be used to re-import
     * old TM), can be {@code null}
     * @return the created {@link TMTextUnitVariant}
     * @throws DataIntegrityViolationException If tmTextUnitId or localeId are
     * invalid
     */
    public TMTextUnitVariant addTMTextUnitVariant(
            Long tmTextUnitId,
            Long localeId,
            String content,
            String comment,
            TMTextUnitVariant.Status status,
            boolean includedInLocalizedFile,
            DateTime createdDate) {

        logger.debug("Add TMTextUnitVariant for tmId: {} locale id: {}, content: {}", tmTextUnitId, localeId, content);

        Preconditions.checkNotNull(content, "content must not be null when adding a TMTextUnitVariant");

        TMTextUnit tmTextUnit = entityManager.getReference(TMTextUnit.class, tmTextUnitId);
        Locale locale = entityManager.getReference(Locale.class, localeId);

        TMTextUnitVariant tmTextUnitVariant = new TMTextUnitVariant();

        tmTextUnitVariant.setTmTextUnit(tmTextUnit);
        tmTextUnitVariant.setLocale(locale);
        tmTextUnitVariant.setContent(content);
        tmTextUnitVariant.setContentMD5(DigestUtils.md5Hex(content));
        tmTextUnitVariant.setComment(comment);
        tmTextUnitVariant.setStatus(status);
        tmTextUnitVariant.setIncludedInLocalizedFile(includedInLocalizedFile);
        tmTextUnitVariant.setCreatedDate(createdDate);
        tmTextUnitVariant.setCreatedByUser(auditorAwareImpl.getCurrentAuditor());
        tmTextUnitVariant = tmTextUnitVariantRepository.save(tmTextUnitVariant);
        logger.trace("TMTextUnitVariant saved");

        return tmTextUnitVariant;
    }

    /**
     * Makes a {@link TMTextUnitVariant} current in a {@link TMTextUnit} for a
     * given locale.
     *
     * @param tmId the TM id (must be the same as the tmTextUnit.getTm()) - used
     * for denormalization
     * @param tmTextUnitId the text unit that will contains the translation
     * @param localeId locale id of the translation
     * @param tmTextUnitVariantId the text unit variant id to be made current
     * @return {@link TMTextUnitCurrentVariant} that contains the
     * {@link TMTextUnitVariant}
     * @throws DataIntegrityViolationException If tmId, tmTextUnitId or localeId
     * are invalid
     */
    protected TMTextUnitCurrentVariant makeTMTextUnitVariantCurrent(Long tmId, Long tmTextUnitId, Long localeId, Long tmTextUnitVariantId) {
        logger.debug("Make the TMTextUnitVariant with id: {} current for locale: {}", tmTextUnitVariantId, localeId);
        TMTextUnitCurrentVariant tmTextUnitCurrentVariant = new TMTextUnitCurrentVariant();
        tmTextUnitCurrentVariant.setTm(entityManager.getReference(TM.class, tmId));
        tmTextUnitCurrentVariant.setTmTextUnit(entityManager.getReference(TMTextUnit.class, tmTextUnitId));
        tmTextUnitCurrentVariant.setTmTextUnitVariant(entityManager.getReference(TMTextUnitVariant.class, tmTextUnitVariantId));
        tmTextUnitCurrentVariant.setLocale(entityManager.getReference(Locale.class, localeId));

        tmTextUnitCurrentVariantRepository.save(tmTextUnitCurrentVariant);
        logger.trace("TMTextUnitCurrentVariant persisted");

        return tmTextUnitCurrentVariant;
    }

    /**
     * Computes a MD5 hash for a {@link TMTextUnit}.
     *
     * @param name the text unit name
     * @param content the text unit content
     * @param comment the text unit comment
     * @return the MD5 hash in Hex
     */
    public String computeTMTextUnitMD5(String name, String content, String comment) {
        return DigestUtils.md5Hex(name + content + comment);
    }

    /**
     * Parses the XLIFF (from a translation kit) content and extract the
     * new/changed variants. Then updates the TM with these new variants.
     *
     * @param xliffContent The content of the localized XLIFF TODO(P1) Use BCP47
     * tag instead of Locale object?
     * @param importStatus specific status to use when importing translation
     * @return the imported XLIFF with information for each text unit about the
     * import process
     * @throws OkapiBadFilterInputException when XLIFF document is invalid
     */
    public UpdateTMWithXLIFFResult updateTMWithTranslationKitXLIFF(
            String xliffContent,
            TMTextUnitVariant.Status importStatus) throws OkapiBadFilterInputException {

        return updateTMWithXliff(xliffContent, importStatus, new ImportTranslationsWithTranslationKitStep());
    }

    public UpdateTMWithXLIFFResult updateTMWithXLIFFById(
            String xliffContent,
            TMTextUnitVariant.Status importStatus) throws OkapiBadFilterInputException {

        return updateTMWithXliff(xliffContent, importStatus, new ImportTranslationsByIdStep());
    }

    /**
     * Parses the XLIFF content and extract the new/changed variants by doing
     * MD5 lookup for a given repository. Then updates the TM with these new
     * variants. If the XLIFF is linked to an existing translation kit, use 
     * {@link #updateTMWithTranslationKitXLIFF(java.lang.String, com.box.l10n.mojito.entity.TMTextUnitVariant.Status) }
     *
     * @param xliffContent The content of the localized XLIFF TODO(P1) Use BCP47
     * tag instead of Locale object?
     * @param importStatus specific status to use when importing translation
     * @param repository the repository in which to perform the import
     * @return the imported XLIFF with information for each text unit about the
     * import process
     * @throws OkapiBadFilterInputException when XLIFF document is invalid
     */
    public UpdateTMWithXLIFFResult updateTMWithXLIFFByMd5(
            String xliffContent,
            TMTextUnitVariant.Status importStatus,
            Repository repository) throws OkapiBadFilterInputException {

        return updateTMWithXliff(xliffContent, importStatus, new ImportTranslationsByMd5Step(repository));
    }

    /**
     * Update TM with XLIFF.
     *
     * @param xliffContent The content of the localized XLIFF TODO(P1) Use BCP47
     * tag instead of Locale object?
     * @param importStatus specific status to use when importing translation
     * @param abstractImportTranslationsStep defines which import logic to apply
     * @return the imported XLIFF with information for each text unit about the
     * import process
     * @throws OkapiBadFilterInputException
     */
    private UpdateTMWithXLIFFResult updateTMWithXliff(
            String xliffContent,
            TMTextUnitVariant.Status importStatus,
            AbstractImportTranslationsStep abstractImportTranslationsStep) throws OkapiBadFilterInputException {

        logger.debug("Configuring pipeline for localized XLIFF processing");

        IPipelineDriver driver = new PipelineDriver();
        driver.addStep(new RawDocumentToFilterEventsStep(new XLIFFFilter()));

        driver.addStep(getConfiguredQualityStep());
        IntegrityCheckStep integrityCheckStep = new IntegrityCheckStep();
        driver.addStep(integrityCheckStep);

        abstractImportTranslationsStep.setImportWithStatus(importStatus);
        driver.addStep(abstractImportTranslationsStep);

        //TODO(P1) It sounds like it's not possible to the XLIFFFilter for the output
        // because the note is readonly mode and we need to override it to provide more information
        logger.debug("Prepare FilterEventsWriterStep to use an XLIFFWriter with outputstream (allows only one doc to be processed)");
        FilterEventsWriterStep filterEventsWriterStep = new FilterEventsWriterStep(new XLIFFWriter());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        filterEventsWriterStep.setOutputStream(byteArrayOutputStream);
        filterEventsWriterStep.setOutputEncoding(StandardCharsets.UTF_8.toString());

        driver.addStep(filterEventsWriterStep);

        // We need to read first the target language, because if we wait for okapi to read
        // it from the file it is too late to write the output with the XLIFFWriter 
        // (missing target language)
        String targetLanguage = xliffUtils.getTargetLanguage(xliffContent);
        LocaleId targetLocaleId = targetLanguage != null ? LocaleId.fromBCP47(targetLanguage) : LocaleId.EMPTY;
        RawDocument rawDocument = new RawDocument(xliffContent, LocaleId.ENGLISH, targetLocaleId);

        driver.addBatchItem(rawDocument, RawDocument.getFakeOutputURIForStream(), null);

        logger.debug("Start processing batch");
        driver.processBatch();

        logger.debug("Get the Import report");
        ImportTranslationsStepAnnotation importTranslationsStepAnnotation = rawDocument.getAnnotation(ImportTranslationsStepAnnotation.class);

        UpdateTMWithXLIFFResult updateReport = new UpdateTMWithXLIFFResult();
        updateReport.setXliffContent(StreamUtil.getUTF8OutputStreamAsString(byteArrayOutputStream));
        updateReport.setComment(importTranslationsStepAnnotation.getComment());

        return updateReport;
    }

    /**
     * @return A {@code QualityCheckStep} that will only perform the needed
     * checks
     */
    private QualityCheckStep getConfiguredQualityStep() {

        Parameters parameters = new Parameters();
        parameters.disableAllChecks();

        // only enable the checks we want
        parameters.setEmptyTarget(true);
        parameters.setTargetSameAsSource(true);
        parameters.setTargetSameAsSourceForSameLanguage(true);
        parameters.setLeadingWS(true);
        parameters.setTrailingWS(true);
        parameters.setDoubledWord(true);
        parameters.setCheckXliffSchema(true);

        QualityCheckStep qualityCheckStep = new QualityCheckStep();
        qualityCheckStep.setParameters(parameters);

        return qualityCheckStep;
    }

    /**
     * Exports an {@link Asset} as XLIFF for a given locale.
     *
     * @param assetId {@link Asset#id} to be exported
     * @param bcp47Tag bcp47tag of the locale that needs to be exported
     * @return an XLIFF that contains {@link Asset}'s translation for that
     * locale
     */
    @Transactional
    public String exportAssetAsXLIFF(Long assetId, String bcp47Tag) {

        logger.debug("Export data for asset id: {} and locale: {}", assetId, bcp47Tag);

        logger.trace("Create XLIFFWriter");
        XLIFFWriter xliffWriter = new XLIFFWriter();

        logger.trace("Prepare FilterEventsWriterStep to use an XLIFFWriter with outputstream (allows only one doc to be processed)");
        FilterEventsWriterStep filterEventsWriterStep = new FilterEventsWriterStep(xliffWriter);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        filterEventsWriterStep.setOutputStream(byteArrayOutputStream);
        filterEventsWriterStep.setOutputEncoding(StandardCharsets.UTF_8.toString());

        logger.trace("Prepare the Okapi pipeline");
        IPipelineDriver driver = new PipelineDriver();
        driver.addStep(new RawDocumentToFilterEventsStep(new TMExportFilter(assetId)));
        driver.addStep(filterEventsWriterStep);

        logger.trace("Add single document with fake output URI to be processed with an outputStream");
        Locale locale = localeService.findByBcp47Tag(bcp47Tag);
        RawDocument rawDocument = new RawDocument(RawDocument.EMPTY, LocaleId.ENGLISH, LocaleId.fromBCP47(locale.getBcp47Tag()));

        driver.addBatchItem(rawDocument, RawDocument.getFakeOutputURIForStream(), null);

        logger.debug("Start processing batch");
        driver.processBatch();

        logger.trace("Get the output result from the stream");
        return StreamUtil.getUTF8OutputStreamAsString(byteArrayOutputStream);
    }

    /**
     * Parses the given content and adds the translation for every text unit.
     * Returns the content of the localized content.
     *
     * TODO(P1) This needs to support other file formats
     *
     * @param asset The {@link Asset} used to get translations
     * @param content The content to be localized
     * @param repositoryLocale the repository locale used to fetch the
     * translation. Also used for the output tag if outputBcp47tag is null.
     * @param outputBcp47tag Optional, can be null. Allows to generate the file
     * for a bcp47 tag that is different from the repository locale (which is
     * still used to fetch the translations). This can be used to generate a
     * file with tag "fr" even if the translations are stored with fr-FR
     * repository locale.
     * @param inheritanceMode
     * @param status
     * @return the localized asset
     */
    public String generateLocalized(
            Asset asset,
            String content,
            RepositoryLocale repositoryLocale,
            String outputBcp47tag,
            FilterConfigIdOverride filterConfigIdOverride,
            InheritanceMode inheritanceMode,
            Status status) {

        String bcp47Tag;

        if (outputBcp47tag == null) {
            bcp47Tag = repositoryLocale.getLocale().getBcp47Tag();
        } else {
            logger.debug("An output bcp47 tag: {} is specified (won't use the default tag (from the repository locale)", outputBcp47tag);
            bcp47Tag = outputBcp47tag;
        }

        logger.debug("Configuring pipeline for localized XLIFF generation");

        BasePipelineStep translateStep = (BasePipelineStep) new TranslateStep(asset, repositoryLocale, inheritanceMode, status);
        return generateLocalizedBase(asset, content, filterConfigIdOverride, bcp47Tag, translateStep);
    }

    /**
     * Parses the given content and adds the pseudo localization for every text
     * unit. Returns the pseudolocalized content.
     *
     * @param asset The {@link Asset} used to get translations
     * @param content The content to be pseudolocalized
     * @return the pseudolocalized asset
     */
    public String generatePseudoLocalized(
            Asset asset,
            String content,
            FilterConfigIdOverride filterConfigIdOverride) {

        String bcp47tag = "en-x-psaccent";

        BasePipelineStep pseudoLocalizedStep = (BasePipelineStep) new PseudoLocalizeStep();
        return generateLocalizedBase(asset, content, filterConfigIdOverride, bcp47tag, pseudoLocalizedStep);
    }

    /**
     * Parses the given content and adds the translation for every text unit.
     * Returns the content of the localized content.
     *
     * TODO(P1) This needs to support other file formats
     *
     * @param asset The {@link Asset} used to get translations
     * @param content The content to be localized
     * @param filterConfigIdOverride
     * @param outputBcp47tag Optional, can be null. Allows to generate the file
     * for a bcp47 tag that is different from the repository locale (which is
     * still used to fetch the translations). This can be used to generate a
     * file with tag "fr" even if the translations are stored with fr-FR
     * repository locale.
     * @param step
     * @return the localized asset
     */
    private String generateLocalizedBase(Asset asset, String content, FilterConfigIdOverride filterConfigIdOverride, String outputBcp47tag, BasePipelineStep step) {

        IPipelineDriver driver = new PipelineDriver();

        driver.addStep(new RawDocumentToFilterEventsStep());
        driver.addStep(new CheckForDoNotTranslateStep());
        driver.addStep(step);

        //TODO(P1) see assetExtractor comments
        logger.debug("Adding all supported filters to the pipeline driver");
        driver.setFilterConfigurationMapper(assetExtractor.getConfiguredFilterConfigurationMapper());

        FilterEventsToInMemoryRawDocumentStep filterEventsToInMemoryRawDocumentStep = new FilterEventsToInMemoryRawDocumentStep();
        driver.addStep(filterEventsToInMemoryRawDocumentStep);

        LocaleId targetLocaleId = LocaleId.fromBCP47(outputBcp47tag);
        RawDocument rawDocument = new RawDocument(content, LocaleId.ENGLISH, targetLocaleId);

        //TODO(P1) see assetExtractor comments
        String filterConfigId;

        if (filterConfigIdOverride != null) {
            filterConfigId = filterConfigIdOverride.getOkapiFilterId();
        } else {
            filterConfigId = assetExtractor.getFilterConfigIdForAsset(asset);
        }

        rawDocument.setFilterConfigId(filterConfigId);
        logger.debug("Set filter config {} for asset {}", filterConfigId, asset.getPath());

        driver.addBatchItem(rawDocument);

        logger.debug("Start processing batch");
        driver.processBatch();

        String localizedContent = filterEventsToInMemoryRawDocumentStep.getOutput(rawDocument);

        return localizedContent;
    }

    /**
     * Imports a localized version of an asset.
     *
     * The target strings are checked against the source strings and if they are
     * equals the status of the imported translation is defined by
     * statusForEqualTarget. When SKIPED is specified the import is actually
     * skipped.
     *
     * For not fully translated locales, targets are imported only if they are
     * different from target of the parent locale.
     *
     * @param asset the asset for which the content will be imported
     * @param content the localized asset content
     * @param repositoryLocale the locale of the content to be imported
     * @param statusForEqualtarget the status of the text unit variant when
     * the source equals the target
     * @param filterConfigIdOverride to override the filter used to process the
     * asset
     * @return
     */
    @Pollable(async = true, message = "Import localized asset")
    public PollableFuture importLocalizedAsset(
            Asset asset,
            String content,
            RepositoryLocale repositoryLocale,
            StatusForEqualTarget statusForEqualtarget,
            FilterConfigIdOverride filterConfigIdOverride) {

        PollableFuture pollableFuture = new PollableFutureTaskResult();

        String bcp47Tag = repositoryLocale.getLocale().getBcp47Tag();

        logger.debug("Configuring pipeline to import localized file");

        IPipelineDriver driver = new PipelineDriver();

        driver.addStep(new RawDocumentToFilterEventsStep());
        driver.addStep(new CheckForDoNotTranslateStep());
        driver.addStep(new ImportTranslationsFromLocalizedAssetStep(asset, repositoryLocale, statusForEqualtarget));

        logger.debug("Adding all supported filters to the pipeline driver");
        driver.setFilterConfigurationMapper(assetExtractor.getConfiguredFilterConfigurationMapper());

        FilterEventsToInMemoryRawDocumentStep filterEventsToInMemoryRawDocumentStep = new FilterEventsToInMemoryRawDocumentStep();
        driver.addStep(filterEventsToInMemoryRawDocumentStep);

        LocaleId targetLocaleId = LocaleId.fromBCP47(bcp47Tag);
        RawDocument rawDocument = new RawDocument(content, LocaleId.ENGLISH, targetLocaleId);
        rawDocument.setAnnotation(new CopyFormsOnImport());

        String filterConfigId;

        if (filterConfigIdOverride != null) {
            filterConfigId = filterConfigIdOverride.getOkapiFilterId();
        } else {
            filterConfigId = assetExtractor.getFilterConfigIdForAsset(asset);
        }

        rawDocument.setFilterConfigId(filterConfigId);
        logger.debug("Set filter config {} for asset {}", filterConfigId, asset.getPath());

        driver.addBatchItem(rawDocument);

        logger.debug("Start processing batch");
        processBatchInTransaction(driver);

        return pollableFuture;
    }

    @Transactional
    void processBatchInTransaction(IPipelineDriver driver) {
        driver.processBatch();
    }

    /**
     * Exports an {@link Asset} as XLIFF for a given locale asynchronously.
     *
     * @param tmXliffId {@link TMXliff#id} to persist generated XLIFF
     * @param assetId {@link Asset#id} to be exported
     * @param bcp47Tag bcp47tag of the locale that needs to be exported
     * @param currentTask
     * @return {@link PollableFutureTaskResult} that contains an XLIFF as result
     */
    @Pollable(async = true, message = "Export asset as xliff")
    public PollableFuture<String> exportAssetAsXLIFFAsync(
            Long tmXliffId,
            Long assetId,
            String bcp47Tag,
            @InjectCurrentTask PollableTask currentTask) {

        PollableFutureTaskResult<String> pollableFutureTaskResult = new PollableFutureTaskResult<>();

        String xliff = exportAssetAsXLIFF(assetId, bcp47Tag);
        String normalized = NormalizationUtils.normalize(xliff);

        TMXliff tmXliff = tmXliffRepository.findOne(tmXliffId);
        tmXliff.setAsset(assetRepository.findOne(assetId));
        tmXliff.setLocale(localeService.findByBcp47Tag(bcp47Tag));
        tmXliff.setContent(normalized);
        tmXliff.setPollableTask(currentTask);
        tmXliffRepository.save(tmXliff);

        pollableFutureTaskResult.setResult(normalized);
        return pollableFutureTaskResult;
    }

    public TMXliff createTMXliff(Long assetId, String bcp47Tag, String content, PollableTask pollableTask) {
        TMXliff tmXliff = new TMXliff();
        tmXliff.setAsset(assetRepository.findOne(assetId));
        tmXliff.setLocale(localeService.findByBcp47Tag(bcp47Tag));
        tmXliff.setContent(content);
        tmXliff.setPollableTask(pollableTask);
        tmXliff = tmXliffRepository.save(tmXliff);
        return tmXliff;
    }
}
