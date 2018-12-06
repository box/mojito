package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.TextUnitUtils;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.ITextUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ITSEnginePatchFilter extends XMLFilter {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ITSEnginePatchFilter.class);

    public static final String FILTER_CONFIG_ID = "its_test@mojito";

    @Override
    public String getName() {
        return FILTER_CONFIG_ID;
    }

    TextUnitUtils textUnitUtils = new TextUnitUtils();


    @Override
    public Event next() {

        Event event = super.next();

        logger.warn("event: {}", event.getEventType().toString());
        if (event.isTextUnit()) {
            ITextUnit textUnit = event.getTextUnit();
            String note = textUnitUtils.getNote(textUnit);
            logger.warn("tu name:\n{} \n\nvalue:\n{}\n\ndesc:\n{}", textUnit.getName(), textUnit.getSource().toString(), note);
        }

        logger.warn("skeleton:\n--- start\n{}\n--- end ", event.getResource().getSkeleton().toString());

        return event;
    }

    /**
     * Overriding to include only resx, xtb and AndroidStrings filters
     *
     * @return
     */
    @Override
    public List<FilterConfiguration> getConfigurations() {
        List<FilterConfiguration> list = new ArrayList<>();
        list.add(new FilterConfiguration(FILTER_CONFIG_ID,
                getMimeType(),
                getClass().getName(),
                "ITSTEST",
                "To test ITS rules",
                "itsenginepatch.fprm",
                ".xml;"));
        return list;
    }
}
