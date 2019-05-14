package com.box.l10n.mojito.okapi;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.ITextUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Step to mark TextUnit as not translatable if it has note 'DO NOT TRANSLATE'.
 *
 * @author jyi
 */
@Configurable
public class CheckForDoNotTranslateStep extends BasePipelineStep {

    static Logger logger = LoggerFactory.getLogger(CheckForDoNotTranslateStep.class);

    /**
     * skip translation if the string has the following comment
     */
    private static final String COMMENT_TO_SKIP_TRANSLATION = "DO NOT TRANSLATE";

    @Autowired
    TextUnitUtils textUnitUtils;

    @Override
    public String getName() {
        return "Do not translate";
    }

    @Override
    public String getDescription() {
        return "Mark TextUnit as not translatable"
                + " if it has note 'DO NOT TRANSLATE'";
    }

    @Override
    protected Event handleTextUnit(Event event) {
        ITextUnit textUnit = event.getTextUnit();
        String comments = textUnitUtils.getNote(textUnit);
        if (StringUtils.contains(comments, COMMENT_TO_SKIP_TRANSLATION)) {
            textUnit.setIsTranslatable(false);
        }
        return event;
    }

}
