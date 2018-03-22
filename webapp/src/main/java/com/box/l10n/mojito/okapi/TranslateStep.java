package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.tm.TranslatorWithInheritance;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
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
    TextUnitSearcher textUnitSearcher;

    private LocaleId targetLocale;

    private Status status;

    private InheritanceMode inheritanceMode;

    Asset asset;

    RepositoryLocale repositoryLocale;

    TranslatorWithInheritance translatorWithInheritance;

    /**
     * Creates the {@link TranslateStep} for a given asset.
     * @param asset {@link Asset} that will be used to lookup translations
     * @param repositoryLocale used to fetch translations. It can be different
     * from the locale used in the Okapi pipeline ({@link #targetLocale}) in
     * case the file needs to be generated for a tag that is different from the
     * locale used for translation.
     * @param inheritanceMode
     * @param status
     */
    public TranslateStep(Asset asset, RepositoryLocale repositoryLocale, InheritanceMode inheritanceMode, Status status) {
        this.asset = asset;
        this.inheritanceMode = inheritanceMode;
        this.repositoryLocale = repositoryLocale;

        StatusFilter statusFilter = getStatusFilter(status);

        this.translatorWithInheritance = new TranslatorWithInheritance(asset, repositoryLocale, inheritanceMode, statusFilter);
    }

    private StatusFilter getStatusFilter(Status status) {
        StatusFilter statusFilter = StatusFilter.TRANSLATED_AND_NOT_REJECTED;

        switch (status) {
            case ALL:
                statusFilter = StatusFilter.TRANSLATED_AND_NOT_REJECTED;
                break;
            case APPROVED:
                statusFilter = StatusFilter.APPROVED_AND_NOT_REJECTED;
                break;
            case APPROVED_OR_NEEDS_REVIEW:
                statusFilter = StatusFilter.APPROVED_OR_NEEDS_REVIEW_AND_NOT_REJECTED;
                break;
        }

        return statusFilter;
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

            String translation = translatorWithInheritance.getTranslation(name, source, md5);

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
