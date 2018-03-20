package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.*;
import com.box.l10n.mojito.service.tm.TranslatorWithInheritance;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author aloison
 */
@Configurable
public class TranslateStep extends AbstractMd5ComputationStep {

    static Logger logger = LoggerFactory.getLogger(TranslateStep.class);

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    @Autowired
    LocaleService localeService;

    @Autowired
    RepositoryLocaleRepository repositoryLocaleRepository;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    private LocaleId targetLocale;

    Asset asset;

    InheritanceMode inheritanceMode;

    RepositoryLocale repositoryLocale;

    TranslatorWithInheritance translatorWithInheritance;

    Map<String, TMTextUnit> textUnitsByMd5 = new HashMap<>();
    Map<String, TMTextUnit> textUnitsByName = new HashMap<>();

    /**
     * Indicates if the document that is processed is multilingual or not
     */
    boolean isMultilingual = false;

    /**
     * Cache that contains the translations required to translate the asset.
     */
    Map<Long, Map<String, TextUnitDTO>> localeToTextUnitDTOsForLocaleMap = new HashMap<>();

    /**
     * Creates the {@link TranslateStep} for a given asset.
     *
     * @param asset {@link Asset} that will be used to lookup translations
     * @param repositoryLocale used to fetch translations. It can be different
     * from the locale used in the Okapi pipeline ({@link #targetLocale}) in
     * case the file needs to be generated for a tag that is different from the
     * locale used for translation.
     * @param inheritanceMode
     */
    public TranslateStep(Asset asset, RepositoryLocale repositoryLocale, InheritanceMode inheritanceMode) {
        this.asset = asset;
        this.inheritanceMode = inheritanceMode;
        this.repositoryLocale = repositoryLocale;
        this.translatorWithInheritance = new TranslatorWithInheritance(asset, repositoryLocale, inheritanceMode);

        initTmTextUnitsMapsForAsset();
    }

    @SuppressWarnings("deprecation")
    @StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
    public void setTargetLocale(LocaleId targetLocale) {
        this.targetLocale = targetLocale;
    }

    @Override
    public String getName() {
        return "Text units translation";
    }

    @Override
    public String getDescription() {
        return "Populates the target with the translations of the TM and state."
                + " Expects: raw document. Sends back: original events.";
    }

    @Override
    protected Event handleStartDocument(Event event) {
        logger.debug("Initialize statistics for the import");

        StartDocument startDocument = event.getStartDocument();
        isMultilingual = startDocument.isMultilingual();

        initTmTextUnitsMapsForAsset();

        return super.handleStartDocument(event);
    }

    void initTmTextUnitsMapsForAsset() {
        logger.debug("Init TmTextUnit maps for asset");
        List<TMTextUnit> textUnits = tmTextUnitRepository.findByAsset(asset);

        for (TMTextUnit tmTextUnit : textUnits) {
            if (isMultilingual) {
                textUnitsByMd5.put(tmTextUnit.getMd5(), tmTextUnit);
            } else {
                textUnitsByName.put(tmTextUnit.getName(), tmTextUnit);
            }
        }
    }

    @Override
    protected Event handleTextUnit(Event event) {
        event = super.handleTextUnit(event);

        if (textUnit.isTranslatable()) {

            String translation = translatorWithInheritance.getTranslation(name, source, md5);

            if (translation == null && InheritanceMode.REMOVE_UNTRANSLATED.equals(inheritanceMode)) {
                logger.debug("Remove untranslated text unit");
                event = Event.NOOP_EVENT;
            } else {
                Long targetLocaleId = getTargetLocaleId();

                logger.debug("Set translation for text unit with name: {}, translation: {}", name, translation);
                TextContainer textContainer = new TextContainer(translation);

                XliffState xliffState = XliffState.NEW;

                TMTextUnit tmTextUnit =  getTMTextUnit();
                TMTextUnitCurrentVariant tmTextUnitCurrentVariant = null;
                TMTextUnitVariant tmTextUnitVariant = null;

                if (tmTextUnit != null) {
                     tmTextUnitCurrentVariant = getTMTextUnitCurrentVariant(targetLocaleId, tmTextUnit);
                }

                if (tmTextUnitCurrentVariant != null) {
                    tmTextUnitVariant = tmTextUnitCurrentVariant.getTmTextUnitVariant();
                }

                if (tmTextUnitVariant != null) {
                    xliffState = getXliffState(tmTextUnitVariant);
                }

                setStateProperty(textContainer, xliffState);

                textUnit.setTarget(targetLocale, textContainer);
            }
        }

        return event;
    }

    TMTextUnit getTMTextUnit() {

        TMTextUnit tmTextUnit;
        if (isMultilingual) {
            tmTextUnit = textUnitsByMd5.get(md5);
        } else {
            tmTextUnit = textUnitsByName.get(name);
        }

        return tmTextUnit;
    }

    Long getTargetLocaleId() {
        String bcp47TagLowerCase = targetLocale.toBCP47();
        return localeService.findByBcp47Tag(bcp47TagLowerCase).getId();
    }

    XliffState getXliffState(TMTextUnitVariant tmTextUnitVariant) {

        XliffState xliffState = XliffState.NEEDS_TRANSLATION;

        if (tmTextUnitVariant.isIncludedInLocalizedFile()) {
            if (TMTextUnitVariant.Status.APPROVED.equals(tmTextUnitVariant.getStatus())) {
                xliffState = XliffState.FINAL;
            } else if (tmTextUnitVariant.getStatus().equals(TMTextUnitVariant.Status.REVIEW_NEEDED)) {
                xliffState = XliffState.NEEDS_REVIEW_TRANSLATION;
            }
        }

        return xliffState;
    }

    TMTextUnitCurrentVariant getTMTextUnitCurrentVariant(Long localeId, TMTextUnit tmTextUnit) {
        return tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(localeId, tmTextUnit.getId());
    }

    /**
     *
     * @param target
     * @param xliffState
     */
    void setStateProperty(TextContainer target, XliffState xliffState) {
        if (target != null) {
            target.setProperty(new net.sf.okapi.common.resource.Property(com.box.l10n.mojito.okapi.Property.STATE, xliffState.toString()));
        }
    }
}
