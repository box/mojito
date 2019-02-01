package com.box.l10n.mojito.scripts;

import java.util.List;
import java.util.Map;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.TextContainer;

/**
 *
 * @author jyi
 */
public class MergeXliffStep extends BasePipelineStep {

    Map<String, List<XliffTextUnit>> xliffTextUnitsInFiles;
    List<XliffTextUnit> currentTextUnitList;
    String currentFileName;
    LocaleId targetLocale;

    public MergeXliffStep(Map<String, List<XliffTextUnit>> xliffTextUnitsInFiles) {
        this.xliffTextUnitsInFiles = xliffTextUnitsInFiles;
        this.targetLocale = LocaleId.fromBCP47("ja-JP");
    }

    @Override
    public String getName() {
        return "MergeXliffStep";
    }

    @Override
    public String getDescription() {
        return "Merges xliff";
    }

    @Override
    protected Event handleStartSubDocument(Event event) {
        StartSubDocument startSubDocument = (StartSubDocument) event.getResource();
        currentFileName = normalizedFileName(startSubDocument.getName());
        currentTextUnitList = xliffTextUnitsInFiles.get(currentFileName);
        return super.handleStartSubDocument(event);
    }

    @Override
    protected Event handleTextUnit(Event event) {
        ITextUnit textUnit = event.getTextUnit();
        XliffTextUnit xliffTextUnit = findXliffTextUnit(textUnit.getName());
        if (xliffTextUnit.getTarget() == null) {
            textUnit.setTarget(targetLocale, new TextContainer(xliffTextUnit.getSource()));
            textUnit.setTargetProperty(targetLocale, new Property("state", "new"));
        } else {
            textUnit.setTarget(targetLocale, new TextContainer(xliffTextUnit.getTarget()));
            textUnit.setTargetProperty(targetLocale, new Property("state", "translated"));
        }
        return super.handleTextUnit(event);
    }

    private String normalizedFileName(String fileName) {
        return fileName.replaceFirst("en/", "");
    }

    private XliffTextUnit findXliffTextUnit(String name) {
        for (XliffTextUnit xliffTextUnit : currentTextUnitList) {
            if (xliffTextUnit.getResname().equals(name)) {
                return xliffTextUnit;
            }
        }
        return null;
    }
}
