package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.android.strings.AndroidStringDocumentMapper;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.AssetMappingService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pluralform.PluralFormService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFile;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.smartling.AssetPathAndTextUnitNameKeys;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.box.l10n.mojito.android.strings.AndroidStringDocumentReader.fromText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/*
* Hybrid integration test that uses a mocked smartling client to perform its operations.
* */
public class ThirdPartyTMSSmartlingTest extends ServiceTestBase {

    static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSSmartlingTest.class);

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Mock
    SmartlingClient smartlingClient;

    @Autowired
    AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    TextUnitSearcher searcher;

    @Autowired
    AssetService assetService;

    @Autowired
    TMService tmService;

    @Autowired
    LocaleService localeService;

    @Autowired
    AssetExtractionService assetExtractionService;

    @Autowired
    AssetExtractionRepository assetExtractionRepository;

    @Autowired
    AssetMappingService assetMappingService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    PluralFormService pluralFormService;

    ThirdPartyTMSSmartling tmsSmartling;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(null).when(smartlingClient).uploadFile(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, assetPathAndTextUnitNameKeys, textUnitSearcher);
    }

    @Test
    public void testPushWithoutSingularsNorPlurals() throws RepositoryNameAlreadyUsedException {

        List<SmartlingFile> result;
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("batchRepo"));

        tmsSmartling.push(repository, "projectId", "_", null, null, Collections.emptyList());
        result = tmsSmartling.getLastPushResult();

        assertThat(result).isEmpty();
        verifyNoInteractions(smartlingClient);
    }

    @Test
    public void testInBatchesWithSingularsAndNoPlurals() throws RepositoryNameAlreadyUsedException {

        int batchSize = 3;
        List<SmartlingFile> result;
        String pluralSep = "_";
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("batchRepo"));
        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, assetPathAndTextUnitNameKeys, textUnitSearcher, batchSize);

        TM tm = repository.getTm();
        Asset asset = assetService.createAssetWithContent(repository.getId(), "fake_for_test", "fake for test");
        AssetExtraction assetExtraction = new AssetExtraction();
        assetExtraction.setAsset(asset);
        assetExtraction = assetExtractionRepository.save(assetExtraction);

        int singularTextUnits = 16;
        int singularBatches = 6;

        for (int i = 0; i < singularTextUnits; i++){
            String name = "singular_message_test" + i;
            String content = "Singular Message Test " + i;
            String comment = "Singular Comment" + i;
            tmService.addTMTextUnit(tm.getId(), asset.getId(), name, content, comment);
            assetExtractionService.createAssetTextUnit(assetExtraction, name, content, comment);
        }

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction.getId(),
                tm.getId(), asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);

        tmsSmartling.push(repository, "projectId", pluralSep, null, null, Collections.emptyList());
        result = tmsSmartling.getLastPushResult();

        assertThat(result).hasSize(singularBatches);
        assertThat(result).allSatisfy(file -> assertThat(file.getFileName()).endsWith("singular_source.xml"));
        assertThat(result.subList(0, 4)).allSatisfy(file -> assertThat(readTextUnits(file, pluralSep)).hasSize(batchSize));
        assertThat(result.get(5)).satisfies(file -> assertThat(readTextUnits(file, pluralSep)).hasSize(singularTextUnits % batchSize));
    }

    @Test
    public void testInBatchesWithNoSingularsAndPlurals() throws RepositoryNameAlreadyUsedException {
        int batchSize = 3;
        List<SmartlingFile> result;
        String pluralSep = "_";
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("batchRepo"));
        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, assetPathAndTextUnitNameKeys, textUnitSearcher, batchSize);

        TM tm = repository.getTm();
        Asset asset = assetService.createAssetWithContent(repository.getId(), "fake_for_test", "fake for test");
        AssetExtraction assetExtraction = new AssetExtraction();
        assetExtraction.setAsset(asset);
        assetExtraction = assetExtractionRepository.save(assetExtraction);

        PluralForm one = pluralFormService.findByPluralFormString("one");

        int pluralTextUnits = 10;
        int pluralBatches = 4;

        for (int i = 0; i < pluralTextUnits; i++){
            String name = "plural_message_test" + i;
            String content = "Plural Message Test " + i;
            String comment = "Plural Comment" + i;
            String pluralFormOther = "plural_form_other" + i;

            tmService.addTMTextUnit(tm, asset, name, content, comment, null, one, pluralFormOther);
            assetExtractionService.createAssetTextUnit(assetExtraction, name, content, comment);
        }

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction.getId(),
                tm.getId(), asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);

        tmsSmartling.push(repository, "projectId", pluralSep, null, null, Collections.emptyList());
        result = tmsSmartling.getLastPushResult();

        assertThat(result).hasSize(pluralBatches);
        assertThat(result).allSatisfy(file -> assertThat(file.getFileName()).endsWith("plural_source.xml"));
        assertThat(result.subList(0, 2)).allSatisfy(file -> assertThat(readTextUnits(file, pluralSep)).hasSize(batchSize));
        assertThat(result.get(3)).satisfies(file -> assertThat(readTextUnits(file, pluralSep)).hasSize(pluralTextUnits % batchSize));
    }

    @Test
    public void testInBatchesWithSingularsAndPlurals() throws RepositoryNameAlreadyUsedException {

        int batchSize = 3;
        List<SmartlingFile> result;
        String pluralSep = "_";
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("batchRepo"));
        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, assetPathAndTextUnitNameKeys, textUnitSearcher, batchSize);

        TM tm = repository.getTm();
        Asset asset = assetService.createAssetWithContent(repository.getId(), "fake_for_test", "fake for test");
        AssetExtraction assetExtraction = new AssetExtraction();
        assetExtraction.setAsset(asset);
        assetExtraction = assetExtractionRepository.save(assetExtraction);

        PluralForm one = pluralFormService.findByPluralFormString("one");

        int singularTextUnits = 14;
        int singularBatches = 5;

        for (int i = 0; i < singularTextUnits; i++){
            String name = "singular_message_test" + i;
            String content = "Singular Message Test " + i;
            String comment = "Singular Comment" + i;
            tmService.addTMTextUnit(tm.getId(), asset.getId(), name, content, comment);
            assetExtractionService.createAssetTextUnit(assetExtraction, name, content, comment);
        }

        int pluralTextUnits = 8;
        int pluralBatches = 3;

        for (int i = 0; i < pluralTextUnits; i++){
            String name = "plural_message_test" + i;
            String content = "Plural Message Test " + i;
            String comment = "Plural Comment" + i;
            String pluralFormOther = "plural_form_other" + i;

            tmService.addTMTextUnit(tm, asset, name, content, comment, null, one, pluralFormOther);
            assetExtractionService.createAssetTextUnit(assetExtraction, name, content, comment);
        }

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction.getId(),
                tm.getId(), asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);

        tmsSmartling.push(repository, "projectId", pluralSep, null, null, Collections.emptyList());
        result = tmsSmartling.getLastPushResult();

        assertThat(result).hasSize(pluralBatches+singularBatches);

        List<SmartlingFile> singularFiles = result.stream()
                .filter(f -> f.getFileName().endsWith("singular_source.xml"))
                .collect(Collectors.toList());

        List<SmartlingFile> pluralFiles = result.stream()
                .filter(f -> f.getFileName().endsWith("plural_source.xml"))
                .collect(Collectors.toList());

        assertThat(singularFiles).hasSize(singularBatches);
        assertThat(singularFiles.subList(0, 3)).allSatisfy(file -> assertThat(readTextUnits(file, pluralSep)).hasSize(batchSize));
        assertThat(singularFiles.get(4)).satisfies(file -> assertThat(readTextUnits(file, pluralSep)).hasSize(singularTextUnits % batchSize));

        assertThat(pluralFiles).hasSize(pluralBatches);
        assertThat(pluralFiles.subList(0, 1)).allSatisfy(file -> assertThat(readTextUnits(file, pluralSep)).hasSize(batchSize));
        assertThat(pluralFiles.get(2)).satisfies(file -> assertThat(readTextUnits(file, pluralSep)).hasSize(pluralTextUnits % batchSize));
    }

    @Test
    public void testPushDryRunNoBatches() {

        List<SmartlingFile> result;
        ThirdPartyServiceTestData testData = new ThirdPartyServiceTestData(testIdWatcher);
        Repository repository = testData.repository;

        tmsSmartling.push(repository, "projectId", "_", null, null, Arrays.asList("dry-run=true"));
        result = tmsSmartling.getLastPushResult();

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(SmartlingFile::getFileName)).containsOnly(
                repository.getName() + "/00000_singular_source.xml",
                repository.getName() + "/00000_plural_source.xml");
        assertThat(result.stream().map(SmartlingFile::getFileContent)).containsOnly(
                singularContent(testData),
                pluralsContent());
        verifyNoInteractions(smartlingClient);
    }

    @Test
    public void testPushWithUploadNoBatches() {

        List<SmartlingFile> result;
        ThirdPartyServiceTestData testData = new ThirdPartyServiceTestData(testIdWatcher);
        Repository repository = testData.repository;
        List<String> optionList = Arrays.asList("smartling-placeholder-format=NONE",
                "smartling-placeholder-format-custom=^some(.*)pattern$");
        tmsSmartling.push(repository, "projectId", "_", null, null, optionList);
        result = tmsSmartling.getLastPushResult();

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(SmartlingFile::getFileName)).containsOnly(
                repository.getName() + "/00000_singular_source.xml",
                repository.getName() + "/00000_plural_source.xml");
        assertThat(result.stream().map(SmartlingFile::getFileContent)).containsOnly(
                singularContent(testData),
                pluralsContent());

        verify(smartlingClient, times(1)).uploadFile(
                eq("projectId"),
                endsWith("singular_source.xml"),
                eq("android"),
                matches("(?s).*string name=.*"),
                eq("NONE"),
                eq("^some(.*)pattern$"));

        verify(smartlingClient, times(1)).uploadFile(
                eq("projectId"),
                endsWith("plural_source.xml"),
                eq("android"),
                matches("(?s).*plurals name=.*"),
                eq("NONE"),
                eq("^some(.*)pattern$"));
    }

    public String singularContent(ThirdPartyServiceTestData testData) {
        String idHello = testData.tmTextUnitHello.getId().toString();
        String idBye = testData.tmTextUnitBye.getId().toString();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                "<!--comment 1-->\n" +
                "<string name=\"src/main/res/values/strings.xml#@#hello\" tmTextUnitId=\"" + idHello +"\">Hello</string>\n" +
                "<!--comment 2-->\n" +
                "<string name=\"src/main/res/values/strings.xml#@#bye\" tmTextUnitId=\"" + idBye +"\">Bye</string>\n" +
                "</resources>\n";
    }

    public String pluralsContent() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                "<plurals name=\"src/main/res/values/strings.xml#@#plural_things\">\n" +
                "<item quantity=\"zero\">Multiple things</item>\n" +
                "<item quantity=\"one\">One thing</item>\n" +
                "<item quantity=\"two\">Multiple things</item>\n" +
                "<item quantity=\"few\">Multiple things</item>\n" +
                "<item quantity=\"many\">Multiple things</item>\n" +
                "<item quantity=\"other\">Multiple things</item>\n" +
                "</plurals>\n" +
                "</resources>\n";
    }

    private List<TextUnitDTO> readTextUnits(SmartlingFile file, String pluralSeparator) {
        AndroidStringDocumentMapper mapper = new AndroidStringDocumentMapper(pluralSeparator, null, null, null);
        List<TextUnitDTO> result;

        try {
            result = mapper.mapToTextUnits(fromText(file.getFileContent()));
        } catch (ParserConfigurationException| IOException| SAXException e) {
            result = new ArrayList<>();
            e.printStackTrace();
        }
        return  result;
    }
}
