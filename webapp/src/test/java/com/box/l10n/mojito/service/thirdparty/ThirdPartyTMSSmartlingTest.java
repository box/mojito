package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.LocaleMappingHelper;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentMapper;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentWriter;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.AssetMappingService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pluralform.PluralFormService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFile;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.smartling.AssetPathAndTextUnitNameKeys;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import java.util.Map;
import java.util.stream.Collectors;

import static com.box.l10n.mojito.android.strings.AndroidStringDocumentReader.fromText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
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

    @Autowired
    TextUnitBatchImporterService textUnitBatchImporterService;

    @Autowired
    LocaleMappingHelper localeMappingHelper;

    @Mock
    TextUnitBatchImporterService mockTextUnitBatchImporterService;

    @Captor
    ArgumentCaptor<List<TextUnitDTO>> textUnitListCaptor;

    ThirdPartyTMSSmartling tmsSmartling;

    AndroidStringDocumentMapper mapper;

    String pluralSep = "_";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(null).when(smartlingClient).uploadFile(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        doReturn(null).when(mockTextUnitBatchImporterService).importTextUnits(any(), eq(false), eq(true));
        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, textUnitSearcher, assetPathAndTextUnitNameKeys, textUnitBatchImporterService);

        mapper = new AndroidStringDocumentMapper(pluralSep, null);
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
    public void testPushInBatchesWithSingularsAndNoPlurals() throws RepositoryNameAlreadyUsedException {

        int batchSize = 3;
        List<SmartlingFile> result;
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("batchRepo"));
        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, textUnitSearcher,
                assetPathAndTextUnitNameKeys, textUnitBatchImporterService, batchSize);

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

        prepareAssetAndTextUnits(assetExtraction, asset, tm);

        tmsSmartling.push(repository, "projectId", pluralSep, null, null, Collections.emptyList());
        result = tmsSmartling.getLastPushResult();

        assertThat(result).hasSize(singularBatches);
        assertThat(result).allSatisfy(file -> assertThat(file.getFileName()).endsWith("singular_source.xml"));
        assertThat(result.subList(0, 4)).allSatisfy(file -> assertThat(readTextUnits(file, pluralSep)).hasSize(batchSize));
        assertThat(result.get(5)).satisfies(file -> assertThat(readTextUnits(file, pluralSep)).hasSize(singularTextUnits % batchSize));
    }

    @Test
    public void testPushInBatchesWithNoSingularsAndPlurals() throws RepositoryNameAlreadyUsedException {
        int batchSize = 3;
        List<SmartlingFile> result;
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("batchRepo"));
        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, textUnitSearcher,
                assetPathAndTextUnitNameKeys, textUnitBatchImporterService, batchSize);

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

        prepareAssetAndTextUnits(assetExtraction, asset, tm);

        tmsSmartling.push(repository, "projectId", pluralSep, null, null, Collections.emptyList());
        result = tmsSmartling.getLastPushResult();

        assertThat(result).hasSize(pluralBatches);
        assertThat(result).allSatisfy(file -> assertThat(file.getFileName()).endsWith("plural_source.xml"));
        assertThat(result.subList(0, 2)).allSatisfy(file -> assertThat(readTextUnits(file, pluralSep)).hasSize(batchSize));
        assertThat(result.get(3)).satisfies(file -> assertThat(readTextUnits(file, pluralSep)).hasSize(pluralTextUnits % batchSize));
    }

    @Test
    public void testPushInBatchesWithSingularsAndPlurals() throws RepositoryNameAlreadyUsedException {

        int batchSize = 3;
        List<SmartlingFile> result;
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("batchRepo"));
        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, textUnitSearcher,
                assetPathAndTextUnitNameKeys, textUnitBatchImporterService, batchSize);

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

        prepareAssetAndTextUnits(assetExtraction, asset, tm);

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
                eq(repository.getName() + "/00000_singular_source.xml"),
                eq("android"),
                matches("(?s).*string name=.*"),
                eq("NONE"),
                eq("^some(.*)pattern$"));

        verify(smartlingClient, times(1)).uploadFile(
                eq("projectId"),
                eq(repository.getName() + "/00000_plural_source.xml"),
                eq("android"),
                matches("(?s).*plurals name=.*"),
                eq("NONE"),
                eq("^some(.*)pattern$"));
    }

    @Test
    public void testPullNoBatches() throws RepositoryLocaleCreationException {
        ThirdPartyServiceTestData testData = new ThirdPartyServiceTestData(testIdWatcher);
        Repository repository = testData.repository;
        Map<String, String> localeMapping = Collections.emptyMap();
        Locale frCA = localeService.findByBcp47Tag("fr-CA");
        Locale jaJP = localeService.findByBcp47Tag("ja-JP");
        repositoryService.addRepositoryLocale(repository, frCA.getBcp47Tag());
        repositoryService.addRepositoryLocale(repository, jaJP.getBcp47Tag());

        doReturn(singularContent(testData, frCA)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(frCA.getBcp47Tag()),
                eq(singularFileName(repository, 0)), eq(false));
        doReturn(singularContent(testData, jaJP)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(jaJP.getBcp47Tag()),
                eq(singularFileName(repository, 0)), eq(false));

        doReturn(pluralsContent(frCA)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(frCA.getBcp47Tag()),
                eq(pluralFileName(repository, 0)), eq(false));
        doReturn(pluralsContent(jaJP)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(jaJP.getBcp47Tag()),
                eq(pluralFileName(repository, 0)), eq(false));

        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, textUnitSearcher,
                assetPathAndTextUnitNameKeys, mockTextUnitBatchImporterService);
        tmsSmartling.pull(repository, "projectId", pluralSep, localeMapping, null, null, Collections.emptyList());

        verify(mockTextUnitBatchImporterService, times(4)).importTextUnits(
                textUnitListCaptor.capture(),
                eq(false),
                eq(true));

        List<List<TextUnitDTO>> captured = textUnitListCaptor.getAllValues();

        assertThat(captured.subList(0,2).stream().flatMap(List::stream))
                .extracting("name", "comment", "target", "assetPath")
                .containsExactlyInAnyOrder(
                        tuple("hello", "comment 1", "Hello in ja-JP", "src/main/res/values/strings.xml"),
                        tuple("bye", "comment 2", "Bye in ja-JP", "src/main/res/values/strings.xml"),
                        tuple("hello", "comment 1", "Hello in fr-CA", "src/main/res/values/strings.xml"),
                        tuple("bye", "comment 2", "Bye in fr-CA", "src/main/res/values/strings.xml"));

        assertThat(captured.subList(2,4).stream().flatMap(List::stream))
                .extracting("name", "target", "pluralForm")
                .containsExactlyInAnyOrder(
                        tuple("plural_things_one", "One thing in ja-JP", "one"),
                        tuple("plural_things_few", "Few things in ja-JP", "few"),
                        tuple("plural_things_other", "Other things in ja-JP", "other"),
                        tuple("plural_things_one", "One thing in fr-CA", "one"),
                        tuple("plural_things_few", "Few things in fr-CA", "few"),
                        tuple("plural_things_other", "Other things in fr-CA", "other"));
    }

    @Test
    public void testPullNoBatchesLocaleMapping() throws RepositoryLocaleCreationException {
        ThirdPartyServiceTestData testData = new ThirdPartyServiceTestData(testIdWatcher);
        Repository repository = testData.repository;
        Map<String, String> localeMapping = localeMappingHelper.getInverseLocaleMapping("fr:fr-CA");
        Locale frCA = localeService.findByBcp47Tag("fr-CA");
        Locale fr = localeService.findByBcp47Tag("fr");
        Locale jaJP = localeService.findByBcp47Tag("ja-JP");
        repositoryService.addRepositoryLocale(repository, frCA.getBcp47Tag());
        repositoryService.addRepositoryLocale(repository, jaJP.getBcp47Tag());

        doReturn(singularContent(testData, fr)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(fr.getBcp47Tag()),
                eq(singularFileName(repository, 0)), eq(false));
        doReturn(singularContent(testData, jaJP)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(jaJP.getBcp47Tag()),
                eq(singularFileName(repository, 0)), eq(false));

        doReturn(pluralsContent(fr)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(fr.getBcp47Tag()),
                eq(pluralFileName(repository, 0)), eq(false));
        doReturn(pluralsContent(jaJP)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(jaJP.getBcp47Tag()),
                eq(pluralFileName(repository, 0)), eq(false));

        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, textUnitSearcher,
                assetPathAndTextUnitNameKeys, mockTextUnitBatchImporterService);
        tmsSmartling.pull(repository, "projectId", pluralSep, localeMapping, null, null, Collections.emptyList());

        verify(mockTextUnitBatchImporterService, times(4)).importTextUnits(
                textUnitListCaptor.capture(),
                eq(false),
                eq(true));

        List<List<TextUnitDTO>> captured = textUnitListCaptor.getAllValues();

        assertThat(captured.subList(0,2).stream().flatMap(List::stream))
                .extracting("name", "comment", "target", "assetPath")
                .containsExactlyInAnyOrder(
                        tuple("hello", "comment 1", "Hello in ja-JP", "src/main/res/values/strings.xml"),
                        tuple("bye", "comment 2", "Bye in ja-JP", "src/main/res/values/strings.xml"),
                        tuple("hello", "comment 1", "Hello in fr", "src/main/res/values/strings.xml"),
                        tuple("bye", "comment 2", "Bye in fr", "src/main/res/values/strings.xml"));

        assertThat(captured.subList(2,4).stream().flatMap(List::stream))
                .extracting("name", "target", "pluralForm")
                .containsExactlyInAnyOrder(
                        tuple("plural_things_one", "One thing in ja-JP", "one"),
                        tuple("plural_things_few", "Few things in ja-JP", "few"),
                        tuple("plural_things_other", "Other things in ja-JP", "other"),
                        tuple("plural_things_one", "One thing in fr", "one"),
                        tuple("plural_things_few", "Few things in fr", "few"),
                        tuple("plural_things_other", "Other things in fr", "other"));
    }

    @Test
    public void testPullNoBatchesPluralFix() throws RepositoryLocaleCreationException {
        ThirdPartyServiceTestData testData = new ThirdPartyServiceTestData(testIdWatcher);
        Repository repository = testData.repository;
        Map<String, String> localeMapping = Collections.emptyMap();
        Locale frCA = localeService.findByBcp47Tag("fr-CA");
        Locale jaJP = localeService.findByBcp47Tag("ja-JP");
        repositoryService.addRepositoryLocale(repository, frCA.getBcp47Tag());
        repositoryService.addRepositoryLocale(repository, jaJP.getBcp47Tag());

        doReturn(singularContent(testData, frCA)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(frCA.getBcp47Tag()),
                eq(singularFileName(repository, 0)), eq(false));
        doReturn(singularContent(testData, jaJP)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(jaJP.getBcp47Tag()),
                eq(singularFileName(repository, 0)), eq(false));

        doReturn(pluralsContent(frCA)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(frCA.getBcp47Tag()),
                eq(pluralFileName(repository, 0)), eq(false));
        doReturn(pluralsContent(jaJP)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(jaJP.getBcp47Tag()),
                eq(pluralFileName(repository, 0)), eq(false));

        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, textUnitSearcher,
                assetPathAndTextUnitNameKeys, mockTextUnitBatchImporterService);
        tmsSmartling.pull(repository, "projectId", pluralSep, localeMapping,
                null, null, ImmutableList.of("smartling-plural-fix=ja-JP"));

        verify(mockTextUnitBatchImporterService, times(4)).importTextUnits(
                textUnitListCaptor.capture(),
                eq(false),
                eq(true));

        List<List<TextUnitDTO>> captured = textUnitListCaptor.getAllValues();

        assertThat(captured.subList(0,2).stream().flatMap(List::stream))
                .extracting("name", "comment", "target", "assetPath")
                .containsExactlyInAnyOrder(
                        tuple("hello", "comment 1", "Hello in ja-JP", "src/main/res/values/strings.xml"),
                        tuple("bye", "comment 2", "Bye in ja-JP", "src/main/res/values/strings.xml"),
                        tuple("hello", "comment 1", "Hello in fr-CA", "src/main/res/values/strings.xml"),
                        tuple("bye", "comment 2", "Bye in fr-CA", "src/main/res/values/strings.xml"));

        assertThat(captured.subList(2,4).stream().flatMap(List::stream))
                .extracting("name", "target", "pluralForm")
                .containsExactlyInAnyOrder(
                        tuple("plural_things_one", "One thing in ja-JP", "one"),
                        tuple("plural_things_few", "Few things in ja-JP", "few"),
                        tuple("plural_things_many", "Other things in ja-JP", "many"),
                        tuple("plural_things_other", "Other things in ja-JP", "other"),
                        tuple("plural_things_one", "One thing in fr-CA", "one"),
                        tuple("plural_things_few", "Few things in fr-CA", "few"),
                        tuple("plural_things_other", "Other things in fr-CA", "other"));
    }

    @Test
    public void testPullDryRunNoBatches() throws RepositoryLocaleCreationException {
        ThirdPartyServiceTestData testData = new ThirdPartyServiceTestData(testIdWatcher);
        Repository repository = testData.repository;
        Map<String, String> localeMapping = ImmutableMap.of("fr", "fr-FR");
        Locale frCA = localeService.findByBcp47Tag("fr-CA");
        Locale jaJP = localeService.findByBcp47Tag("ja-JP");
        repositoryService.addRepositoryLocale(repository, frCA.getBcp47Tag());
        repositoryService.addRepositoryLocale(repository, jaJP.getBcp47Tag());

        doReturn(singularContent(testData)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(frCA.getBcp47Tag()),
                eq(singularFileName(repository, 0)), eq(false));
        doReturn(singularContent(testData)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(jaJP.getBcp47Tag()),
                eq(singularFileName(repository, 0)), eq(false));

        doReturn(pluralsContent()).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(frCA.getBcp47Tag()),
                eq(pluralFileName(repository, 0)), eq(false));
        doReturn(pluralsContent()).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(jaJP.getBcp47Tag()),
                eq(pluralFileName(repository, 0)), eq(false));

        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, textUnitSearcher,
                assetPathAndTextUnitNameKeys, mockTextUnitBatchImporterService);
        tmsSmartling.pull(repository, "projectId", pluralSep, localeMapping, null, null, ImmutableList.of("dry-run=true"));

        verify(mockTextUnitBatchImporterService, never()).importTextUnits(
                textUnitListCaptor.capture(),
                eq(false),
                eq(true));
    }

    @Test
    public void testPullInBatchesWithSingularsAndPlurals() throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException {

        int batchSize = 3;
        List<TextUnitDTO> tus;
        List<Locale> locales = new ArrayList<>();
        Map<String, String> localeMapping = Collections.emptyMap();
        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, textUnitSearcher,
                assetPathAndTextUnitNameKeys, mockTextUnitBatchImporterService, batchSize);
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("batchRepo"));
        Locale frCA = localeService.findByBcp47Tag("fr-CA");
        Locale jaJP = localeService.findByBcp47Tag("ja-JP");
        repositoryService.addRepositoryLocale(repository, frCA.getBcp47Tag());
        repositoryService.addRepositoryLocale(repository, jaJP.getBcp47Tag());
        locales.add(frCA);
        locales.add(jaJP);

        TM tm = repository.getTm();
        Asset asset = assetService.createAssetWithContent(repository.getId(), "fake_for_test", "fake for test");
        AssetExtraction assetExtraction = new AssetExtraction();
        assetExtraction.setAsset(asset);
        assetExtraction = assetExtractionRepository.save(assetExtraction);

        PluralForm one = pluralFormService.findByPluralFormString("one");

        List<Long> singularIds = new ArrayList<>();
        int singularTextUnits = 14;
        int singularBatches = 5;

        for (int i = 0; i < singularTextUnits; i++){
            String name = "singular_message_test" + i;
            String content = "Singular Message Test #" + i;
            String comment = "Singular Comment" + i;
            TMTextUnit textUnit = tmService.addTMTextUnit(tm.getId(), asset.getId(), name, content, comment);
            singularIds.add(textUnit.getId());
            assetExtractionService.createAssetTextUnit(assetExtraction, name, content, comment);
        }

        List<Long> pluralIds = new ArrayList<>();
        int pluralTextUnits = 8;
        int pluralBatches = 3;

        for (int i = 0; i < pluralTextUnits; i++){
            String name = "plural_message" + i + "_one";
            String content = "Plural Message Test #" + i;
            String comment = "Plural Comment" + i;
            String pluralFormOther = "plural_form_other" + i;

            TMTextUnit textUnit = tmService.addTMTextUnit(tm, asset, name, content, comment, null, one, pluralFormOther);
            pluralIds.add(textUnit.getId());
            assetExtractionService.createAssetTextUnit(assetExtraction, name, content, comment);
        }

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction.getId(), tm.getId(), asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);

        tus = searchTextUnits(singularIds);
        Iterable<List<TextUnitDTO>> singularIt = Iterables.partition(tus, batchSize);
        int i = 0;
        for (List<TextUnitDTO> textUnits : singularIt){
            for (Locale l : locales){
                doReturn(localizedContent(textUnits, l)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(l.getBcp47Tag()),
                        eq(singularFileName(repository, i)), eq(false));
            }
            i++;
        }

        tus = searchTextUnits(pluralIds);
        Iterable<List<TextUnitDTO>> pluralIt = Iterables.partition(tus, batchSize);
        i = 0;
        for (List<TextUnitDTO> textUnits : pluralIt){
            for (Locale l : locales){
                doReturn(localizedContent(textUnits, l)).when(smartlingClient).downloadPublishedFile(eq("projectId"), eq(l.getBcp47Tag()),
                        eq(pluralFileName(repository, i)), eq(false));
            }
            i++;
        }

        tmsSmartling.pull(repository, "projectId", pluralSep, localeMapping, null, null, Collections.emptyList());

        verify(mockTextUnitBatchImporterService, atLeastOnce()).importTextUnits(
                textUnitListCaptor.capture(),
                eq(false),
                eq(true));

        List<List<TextUnitDTO>> captured = textUnitListCaptor.getAllValues();

        assertThat(captured).hasSize((pluralBatches+singularBatches)*locales.size());
        assertThat(captured.subList(0,8)).allSatisfy(list -> assertThat(list).hasSize(batchSize));
        assertThat(captured.subList(8,10)).allSatisfy(list -> assertThat(list).hasSize(singularTextUnits % batchSize));
        assertThat(captured.subList(10,14)).allSatisfy(list -> assertThat(list).hasSize(batchSize));
        assertThat(captured.subList(14,16)).allSatisfy(list -> assertThat(list).hasSize(pluralTextUnits % batchSize));
    }

    @Test
    public void testBatchesFor(){

        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, textUnitSearcher,
                assetPathAndTextUnitNameKeys, mockTextUnitBatchImporterService, 3);

        assertThat(tmsSmartling.batchesFor(0)).isEqualTo(0);
        assertThat(tmsSmartling.batchesFor(1)).isEqualTo(1);
        assertThat(tmsSmartling.batchesFor(2)).isEqualTo(1);
        assertThat(tmsSmartling.batchesFor(3)).isEqualTo(1);
        assertThat(tmsSmartling.batchesFor(5)).isEqualTo(2);
        assertThat(tmsSmartling.batchesFor(6)).isEqualTo(2);
        assertThat(tmsSmartling.batchesFor(13)).isEqualTo(5);
        assertThat(tmsSmartling.batchesFor(32)).isEqualTo(11);

        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, textUnitSearcher,
                assetPathAndTextUnitNameKeys, mockTextUnitBatchImporterService, 35);

        assertThat(tmsSmartling.batchesFor(0)).isEqualTo(0);
        assertThat(tmsSmartling.batchesFor(1)).isEqualTo(1);
        assertThat(tmsSmartling.batchesFor(2)).isEqualTo(1);
        assertThat(tmsSmartling.batchesFor(34)).isEqualTo(1);
        assertThat(tmsSmartling.batchesFor(72)).isEqualTo(3);
        assertThat(tmsSmartling.batchesFor(290)).isEqualTo(9);

        tmsSmartling = new ThirdPartyTMSSmartling(smartlingClient, textUnitSearcher,
                assetPathAndTextUnitNameKeys, mockTextUnitBatchImporterService, 4231);

        assertThat(tmsSmartling.batchesFor(0)).isEqualTo(0);
        assertThat(tmsSmartling.batchesFor(1)).isEqualTo(1);
        assertThat(tmsSmartling.batchesFor(2)).isEqualTo(1);
        assertThat(tmsSmartling.batchesFor(2900)).isEqualTo(1);
        assertThat(tmsSmartling.batchesFor(21156)).isEqualTo(6);
    }

    private List<TextUnitDTO> searchTextUnits(List<Long> ids){
        TextUnitSearcherParameters params = new TextUnitSearcherParameters();
        params.setTmTextUnitIds(ids);
        params.setRootLocaleExcluded(false);
        params.setStatusFilter(StatusFilter.TRANSLATED);
        return searcher.search(params);
    }

    public String singularFileName(Repository repository, long num) {
        return SmartlingFileUtils.getOutputSourceFile(num, repository.getName(), "singular");
    }

    public String pluralFileName(Repository repository, long num) {
        return SmartlingFileUtils.getOutputSourceFile(num, repository.getName(), "plural");
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

    public String localizedContent(List<TextUnitDTO> input, Locale locale) {
        try {
            List<TextUnitDTO> list = input.stream()
                    .peek(tu -> tu.setTarget(tu.getTarget() + " in " + locale.getBcp47Tag()))
                    .collect(Collectors.toList());
            return new AndroidStringDocumentWriter(mapper.readFromTargetTextUnits(list)).toText();
        } catch (Exception e) {
            return "";
        }
    }

    public String singularContent(ThirdPartyServiceTestData testData, Locale locale) {
        String idHello = testData.tmTextUnitHello.getId().toString();
        String idBye = testData.tmTextUnitBye.getId().toString();
        String hello = "Hello in " + locale.getBcp47Tag();
        String bye = "Bye in " + locale.getBcp47Tag();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                "<!--comment 1-->\n" +
                "<string name=\"src/main/res/values/strings.xml#@#hello\" tmTextUnitId=\"" + idHello +"\">" + hello + "</string>\n" +
                "<!--comment 2-->\n" +
                "<string name=\"src/main/res/values/strings.xml#@#bye\" tmTextUnitId=\"" + idBye +"\">" + bye + "</string>\n" +
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

    public String pluralsContent(Locale locale) {
        String localeTag = locale.getBcp47Tag();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                "<plurals name=\"src/main/res/values/strings.xml#@#plural_things\">\n" +
                "<item quantity=\"one\">One thing in " + localeTag + "</item>\n" +
                "<item quantity=\"few\">Few things in " + localeTag + "</item>\n" +
                "<item quantity=\"other\">Other things in " + localeTag + "</item>\n" +
                "</plurals>\n" +
                "</resources>\n";
    }

    public String pluralsContent(Locale locale, int num) {
        String localeTag = locale.getBcp47Tag();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                "<plurals name=\"src/main/res/values/strings.xml#@#plural_things\">\n" +
                "<item quantity=\"one\">One thing in " + localeTag + "</item>\n" +
                "<item quantity=\"few\">Few things in " + localeTag + "</item>\n" +
                "<item quantity=\"other\">Other things in " + localeTag + "</item>\n" +
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

    private void prepareAssetAndTextUnits(AssetExtraction assetExtraction, Asset asset, TM tm) {
        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction.getId(),
                tm.getId(), asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);
    }

}
