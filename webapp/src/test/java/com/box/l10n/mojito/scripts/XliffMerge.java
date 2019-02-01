package com.box.l10n.mojito.scripts;

import com.box.l10n.mojito.common.StreamUtil;
import com.box.l10n.mojito.okapi.RawDocument;
import com.box.l10n.mojito.okapi.XLIFFWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import net.sf.okapi.steps.common.FilterEventsWriterStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

/**
 *
 * @author jyi
 */
public class XliffMerge {

    File sourceFile;
    File localizedFile;
    File mergedFile;
    Map<String, Integer> tuCountInFiles;
    Map<String, String> report;
    Map<String, List<XliffTextUnit>> xliffTextUnitsInFiles;

    public void setup() throws IOException {
        report = new TreeMap<>();

        sourceFile = new File("/Users/jyi/Dev/moji/devsite/tm/v2.0/devsite-extracted_en-US.xliff");
        localizedFile = new File("/Users/jyi/Dev/moji/devsite/tm/v2.0/devsite-extracted_ja-JP.xliff");
        mergedFile = new File("/Users/jyi/Dev/moji/devsite/tm/v2.0/devsite-merged.xliff");

        if (!sourceFile.exists()) {
            System.err.println("Source file does not exist: " + sourceFile.getAbsolutePath());
        }

        if (!localizedFile.exists()) {
            System.err.println("Localized file does not exist: " + localizedFile.getAbsolutePath());
        }

        if (!mergedFile.exists()) {
            System.err.println("Localized file does not exist: " + mergedFile.getAbsolutePath());
        }
    }

    public void processSourceFile() throws Exception {
        System.out.println("===============================");
        System.out.println("Start processing source file");

        ProcessSourceTranslationUnitStep step = new ProcessSourceTranslationUnitStep();
        IPipelineDriver driver = new PipelineDriver();
        driver.addStep(new RawDocumentToFilterEventsStep(new XLIFFFilter()));
        driver.addStep(step);

        String content = FileUtils.readFileToString(sourceFile, "utf-8");
        RawDocument rawDocument = new RawDocument(content, LocaleId.ENGLISH);
        driver.addBatchItem(rawDocument);
        driver.processBatch();

        xliffTextUnitsInFiles = step.getXliffTextUnitsInFiles();
        tuCountInFiles = step.getTuCountInFiles();
        for (String file : tuCountInFiles.keySet()) {
            System.out.println(file + ": " + tuCountInFiles.get(file));
        }

        System.out.println("Done processing source file");
    }

    public void processLocalizedFile() throws Exception {
        System.out.println("===============================");
        System.out.println("Start processing localized file");

        ProcessTargetTranslationUnitStep step = new ProcessTargetTranslationUnitStep(xliffTextUnitsInFiles, tuCountInFiles, report);
        IPipelineDriver driver = new PipelineDriver();
        driver.addStep(new RawDocumentToFilterEventsStep(new XLIFFFilter()));
        driver.addStep(step);

        String content = FileUtils.readFileToString(localizedFile, "utf-8");
        RawDocument rawDocument = new RawDocument(content, LocaleId.fromBCP47("ja-JP"));
        driver.addBatchItem(rawDocument);
        driver.processBatch();

        report = step.getReport();
        for (String file : report.keySet()) {
            System.out.println(file + ": " + report.get(file));
        }

        System.out.println("");
        System.out.println("Total file error count: " + step.getFileErrorCount());
        System.out.println("Total TU mismatch count: " + step.getTuMismatchCount());
        System.out.println("Total file missing count: " + step.getFileMissingCount());
        System.out.println("");

        System.out.println("Done processing localized file");
    }

    public void writeMergedXliff() throws Exception {
        System.out.println("===============================");
        System.out.println("Start writing merged xliff file");

        XLIFFWriter xliffWriter = new XLIFFWriter();
        FilterEventsWriterStep filterEventsWriterStep = new FilterEventsWriterStep(xliffWriter);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        filterEventsWriterStep.setOutputStream(byteArrayOutputStream);
        filterEventsWriterStep.setOutputEncoding(StandardCharsets.UTF_8.toString());

        MergeXliffStep step = new MergeXliffStep(xliffTextUnitsInFiles);
        IPipelineDriver driver = new PipelineDriver();
        driver.addStep(new RawDocumentToFilterEventsStep(new XLIFFFilter()));
        driver.addStep(step);
        driver.addStep(filterEventsWriterStep);

        String content = FileUtils.readFileToString(sourceFile, "utf-8");
        RawDocument rawDocument = new RawDocument(content, LocaleId.ENGLISH, LocaleId.fromBCP47("ja-JP"));
        driver.addBatchItem(rawDocument, RawDocument.getFakeOutputURIForStream(), null);
        driver.processBatch();

        FileUtils.writeStringToFile(mergedFile, StreamUtil.getUTF8OutputStreamAsString(byteArrayOutputStream), "utf-8");

        System.out.println("Done writing merged xliff file");
    }

    @Test
    public void processXliff() throws Exception {
        setup();

        processSourceFile();
        processLocalizedFile();

        writeMergedXliff();

    }

}
