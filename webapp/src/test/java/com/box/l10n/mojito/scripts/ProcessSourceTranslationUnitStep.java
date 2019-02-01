package com.box.l10n.mojito.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartSubDocument;

/**
 *
 * @author jyi
 */
public class ProcessSourceTranslationUnitStep extends BasePipelineStep {

    Map<String, List<XliffTextUnit>> xliffTextUnitsInFiles = new TreeMap<>();
    Map<String, Integer> tuCountInFiles = new TreeMap<>();
    List<XliffTextUnit> currentTextUnitList;
    String currentFileName;
    int tuCount;

    public Map<String, List<XliffTextUnit>> getXliffTextUnitsInFiles() {
        return xliffTextUnitsInFiles;
    }

    public Map<String, Integer> getTuCountInFiles() {
        return tuCountInFiles;
    }

    @Override
    public String getName() {
        return "ProcessSourceTranslationUnitStep";
    }

    @Override
    public String getDescription() {
        return "Process source translation units";
    }

    @Override
    protected Event handleStartSubDocument(Event event) {
        StartSubDocument startSubDocument = (StartSubDocument) event.getResource();
        currentTextUnitList = new ArrayList<>();
        currentFileName = normalizedFileName(startSubDocument.getName());
        tuCount = 0;
        return super.handleStartSubDocument(event);
    }

    @Override
    protected Event handleTextUnit(Event event) {
        ITextUnit textUnit = event.getTextUnit();
        XliffTextUnit xliffTextUnit = new XliffTextUnit();
        xliffTextUnit.setId(textUnit.getId());
        xliffTextUnit.setResname(textUnit.getName());
        xliffTextUnit.setSource(textUnit.getSource().toString());
        currentTextUnitList.add(xliffTextUnit);
        tuCount++;
        return super.handleTextUnit(event);
    }

    @Override
    protected Event handleEndSubDocument(Event event) {
        xliffTextUnitsInFiles.put(currentFileName, currentTextUnitList);
        tuCountInFiles.put(currentFileName, tuCount);
        return super.handleEndSubDocument(event);
    }

    private String normalizedFileName(String fileName) {
        return fileName.replaceFirst("en/", "");
    }
}
