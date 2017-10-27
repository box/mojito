package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.service.tm.TranslatorWithInheritance;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import java.util.HashMap;
import java.util.Map;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
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
        return "Populates the target with the translations of the TM."
                + " Expects: raw document. Sends back: original events.";
    }

    @Override
    protected Event handleTextUnit(Event event) {
        event = super.handleTextUnit(event);

        if (textUnit.isTranslatable()) {

            String translation = translatorWithInheritance.getTranslation(name, source, md5, repositoryLocale, inheritanceMode);

            if (translation == null && InheritanceMode.REMOVE_UNTRANSLATED.equals(inheritanceMode)) {
                logger.debug("Remove untranslated text unit");
                event = Event.NOOP_EVENT;
            } else {
                logger.debug("Set translation for text unit with name: {}, translation: {}", name, translation);
                textUnit.setTarget(targetLocale, new TextContainer(translation));
            }
        }

        return event;
    }
}
