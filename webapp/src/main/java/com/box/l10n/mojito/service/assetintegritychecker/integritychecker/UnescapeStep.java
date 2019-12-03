package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.okapi.filters.AndroidFilter;
import com.box.l10n.mojito.okapi.asset.AssetPathToFilterConfigMapper;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * This is to fix any escaped double-quotes and single quotes in target.
 * @author jyi
 */
@Configurable
public class UnescapeStep extends BasePipelineStep {
    
    /**
     * Logger
     */
    static Logger logger = LoggerFactory.getLogger(UnescapeStep.class);

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    AssetPathToFilterConfigMapper assetPathToFilterConfigMapper;

    private LocaleId targetLocale;

    @SuppressWarnings("deprecation")
    @StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
    public void setTargetLocale(LocaleId targetLocale) {
        this.targetLocale = targetLocale;
    }

    @Override
    public String getName() {
        return "Unescape";
    }

    @Override
    public String getDescription() {
        return "Fixes escaped double-quotes and single-quotes for AndroidStrings and MacStrings";
    }

    @Override
    protected Event handleTextUnit(Event event) {

        ITextUnit textUnit = event.getTextUnit();

        if (textUnit.isTranslatable()) {
            TMTextUnit tmTextUnit = null;
            Asset asset = null;

            try {
                Long tmTextUnitId = Long.valueOf(textUnit.getId());
                tmTextUnit = tmTextUnitRepository.findOne(tmTextUnitId);
            } catch (NumberFormatException nfe) {
                logger.debug("Could not convert the textUnit id into a Long (TextUnit id)", nfe);
            }

            if (tmTextUnit != null) {
                asset = tmTextUnit.getAsset();
                try {
                    String filterConfigId = assetPathToFilterConfigMapper.getFilterConfigIdFromPath(asset.getPath());
                    if (AndroidFilter.FILTER_CONFIG_ID.equals(filterConfigId)
                            || AssetPathToFilterConfigMapper.MACSTRINGS_FILTER_CONFIG_ID.equals(filterConfigId)) {
                        String targetContent = textUnit.getTarget(targetLocale).toString();
                        targetContent = unescape(targetContent);
                        TextContainer target = new TextContainer(targetContent);
                        textUnit.setTarget(targetLocale, target);
                    }
                } catch (UnsupportedAssetFilterTypeException ex) {
                    logger.debug("Could not find the asset filter type", ex);
                }
            }
        }

        return event;
    }
    
    private String unescape(String text) {
        text = text.replaceAll("\\\\(\"|')", "$1");
        return text;
    }
}
