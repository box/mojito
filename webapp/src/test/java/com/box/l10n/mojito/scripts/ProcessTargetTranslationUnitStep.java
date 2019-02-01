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
public class ProcessTargetTranslationUnitStep extends BasePipelineStep {

    Map<String, List<XliffTextUnit>> xliffTextUnitsInFiles;
    Map<String, Integer> tuCountInFiles = new TreeMap<>();
    Map<String, String> report = new TreeMap<>();
    List<String> currentTranslationList;
    String currentFileName;
    int tuCount;
    int fileError = 0;
    int tuMismatchCount = 0;
    int fileCount = 0;

    public ProcessTargetTranslationUnitStep(Map<String, List<XliffTextUnit>> xliffTextUnitsInFiles, Map<String, Integer> tuCountInFiles, Map<String, String> report) {
        this.xliffTextUnitsInFiles = xliffTextUnitsInFiles;
        this.tuCountInFiles = tuCountInFiles;
        this.report = report;
    }

    public Map<String, List<XliffTextUnit>> getXliffTextUnitsInFiles() {
        return xliffTextUnitsInFiles;
    }

    public Map<String, String> getReport() {
        return report;
    }

    public int getFileErrorCount() {
        return fileError;
    }

    public int getTuMismatchCount() {
        return tuMismatchCount;
    }

    public int getFileMissingCount() {
        return tuCountInFiles.size() - fileCount;
    }

    @Override
    public String getName() {
        return "ProcessTargetTranslationUnitStep";
    }

    @Override
    public String getDescription() {
        return "Processes target translation units";
    }

    @Override
    protected Event handleStartSubDocument(Event event) {
        StartSubDocument startSubDocument = (StartSubDocument) event.getResource();
        currentTranslationList = new ArrayList<>();
        currentFileName = normalizedFileName(startSubDocument.getName());
        tuCount = 0;
        return super.handleStartSubDocument(event);
    }

    @Override
    protected Event handleTextUnit(Event event) {
        ITextUnit textUnit = event.getTextUnit();
        currentTranslationList.add(textUnit.getSource().toString());
        tuCount++;
        return super.handleTextUnit(event);
    }

    @Override
    protected Event handleEndSubDocument(Event event) {
        Integer expectedTuCount = tuCountInFiles.get(currentFileName);
        if (expectedTuCount == null) {
            report.put(currentFileName, "NOT FOUND");
            fileError++;
        } else if (tuCount == expectedTuCount) {
            List<XliffTextUnit> currentXliffTextUnitList = xliffTextUnitsInFiles.get(currentFileName);
            for (int i = 0; i < currentXliffTextUnitList.size(); i++) {
                XliffTextUnit xliffTextUnit = currentXliffTextUnitList.get(i);
                xliffTextUnit.setTarget(currentTranslationList.get(i));
            }
            xliffTextUnitsInFiles.put(currentFileName, currentXliffTextUnitList);
            report.put(currentFileName, "OK");
        } else {
            report.put(currentFileName, "TU COUNT MISMATCH");
            tuMismatchCount++;
        }
        fileCount++;
        return super.handleEndSubDocument(event);
    }

    private String normalizedFileName(String fileName) {
        return fileName.replaceFirst("ja/", "");
    }
}
