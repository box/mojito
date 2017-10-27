package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.service.tm.TMService;
import com.google.common.base.Strings;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Step to compute md5 from the {@link TextUnit}.
 * 
 * @author jyi
 */
@Configurable
public abstract class AbstractMd5ComputationStep extends BasePipelineStep {
    
    /**
     * Logger
     */
    static Logger logger = LoggerFactory.getLogger(AbstractMd5ComputationStep.class);
    
    /**
     * when developer does not provide comment, some tools auto-generate comment
     * auto-generated comments should be ignored
     */
    private static final String COMMENT_TO_IGNORE = "No comment provided by engineer";
    
    @Autowired
    TextUnitUtils textUnitUtils;
    
    @Autowired
    TMService tmService;
    
    protected String name;
    protected String source;
    protected String comments;
    protected String md5;
    protected ITextUnit textUnit;

    @Override
    protected Event handleTextUnit(Event event) {
        textUnit = event.getTextUnit();

        if (textUnit.isTranslatable()) {
            name = Strings.isNullOrEmpty(textUnit.getName()) ? textUnit.getId() : textUnit.getName();
            source = textUnit.getSource().toString();
            comments = textUnitUtils.getNote(textUnit);
            if (comments != null && comments.contains(COMMENT_TO_IGNORE)) {
                comments = null;
            }
            md5 = tmService.computeTMTextUnitMD5(name, source, comments);
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Handle text unit with name: {}\nsource: {}\ncomments: {}\nmd5: {}", name, source, comments, md5);
        }

        return event;
    }    
}
