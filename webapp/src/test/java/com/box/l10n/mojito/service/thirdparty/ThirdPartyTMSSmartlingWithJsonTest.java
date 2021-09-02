package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetcontent.AssetContentService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingJsonConverter;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingOptions;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.SmartlingJsonKeys;
import com.box.l10n.mojito.smartling.SmartlingTestConfig;
import com.box.l10n.mojito.smartling.response.File;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;

public class ThirdPartyTMSSmartlingWithJsonTest extends ServiceTestBase {

    static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSSmartlingWithJsonTest.class);

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Autowired(required = false)
    SmartlingClient smartlingClient;

    @Autowired
    SmartlingTestConfig testConfig;

    @Autowired(required = false)
    ThirdPartyTMSSmartlingWithJson thirdPartyTMSSmartlingWithJson;

    @Mock
    ThirdPartyTMSSmartlingWithJson thirdPartyTMSSmartlingWithJsonMock;

    @Autowired(required = false)
    ThirdPartyService thirdPartyService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetService assetService;

    @Autowired
    AssetContentService assetContentService;

    @Autowired
    AssetExtractionService assetExtractionService;

    @Autowired
    TextUnitBatchImporterService textUnitBatchImporterService;

    @Mock
    TextUnitBatchImporterService textUnitBatchImporterServiceMock;

    @Autowired
    TextUnitSearcher textUnitSearcher;


    SmartlingJsonConverter smartlingJsonConverter = new SmartlingJsonConverter(ObjectMapper.withIndentedOutput(), new SmartlingJsonKeys());

    @Test
    public void testJsonWithICUMessagFormats() throws Exception {
        Assume.assumeNotNull(smartlingClient);
        Assume.assumeNotNull(testConfig.projectId);

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocaleFrFR = repositoryService.addRepositoryLocale(repository, "fr-FR");
        RepositoryLocale repoLocalejaJP = repositoryService.addRepositoryLocale(repository, "ja-JP");

        String assetContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<resources>\n" +
                "    <!--comment 1-->\n" +
                "    <string name=\"hello\">Hello</string>\n" +
                "    <!--comment 2-->\n" +
                "    <string name=\"bye\">Bye</string>\n" + "" +
                "    <!--comment 2-->\n" +
                "    <string name=\"bye\">Bye duplicate key</string>\n" + "" +
                "    <!--test message format 1-->\n" +
                "    <string name=\"messageformat_1\">{numCats,plural,one{# cat}other{# cats}}</string>\n" +
                "    <!--test message format 2-->\n" +
                "    <string name=\"messageformat_2\">{numCats,plural,one{# cat}other{# cats}} and {numDogs,plural,one{# dog}other{# dogs}}</string>\n" +
                "    <!--test message format 3-->\n" +
                "    <string name=\"messageformat_3\">Hello {name}! {numCats,plural,one{there is # cat}other{There are # cats}}</string>\n" +
                "</resources>";

        Asset asset = assetService.createAssetWithContent(repository.getId(), "src/main/res/values/strings.xml", assetContent);

        AssetContent assetContentEntity = assetContentService.createAssetContent(asset, assetContent);
        assetExtractionService.processAssetAsync(assetContentEntity.getId(), null, null, null).get();

        thirdPartyTMSSmartlingWithJson.getRepositoryLocaleWithoutRootStream(repository)
                .forEach(repositoryLocale -> {
                    logger.debug("Translate for test, locale: {}", repositoryLocale.getLocale().getBcp47Tag());

                    ImmutableList<TextUnitDTO> localizedTextUnitDTOs = StreamSupport.stream(thirdPartyTMSSmartlingWithJson.getSourceTextUnitIterator(repository, null, null), false)
                            .map(textUnitDTO -> {
                                String target = textUnitDTO.getSource().replace("source", "target") + "-" + repositoryLocale.getLocale().getBcp47Tag();
                                textUnitDTO.setTarget(target);
                                textUnitDTO.setTargetLocale(repositoryLocale.getLocale().getBcp47Tag());
                                return textUnitDTO;
                            })
                            .collect(ImmutableList.toImmutableList());
                    textUnitBatchImporterService.importTextUnits(localizedTextUnitDTOs, false, false);
                });

        SmartlingOptions smartlingOptions = SmartlingOptions.parseList(ImmutableList.of());
        thirdPartyTMSSmartlingWithJson.push(repository, testConfig.projectId, null, null, null, smartlingOptions);

        waitForCondition("eventually we should be able to push translation", () -> {
            logger.debug("Pushing translation...");
            thirdPartyTMSSmartlingWithJson.pushTranslations(repository, testConfig.projectId, null, ImmutableMap.of(), null, null, smartlingOptions);
            return true;
        }, 3, 1000);

        waitForCondition("eventually we should be able to pull", () -> {
            logger.debug("Pulling...");
            thirdPartyTMSSmartlingWithJson.pull(repository, testConfig.projectId, ImmutableMap.of());
            return true;
        }, 3, 1000);

        // Smartling rewrites the message formats so the translation are not exactly as imported
        assertThat(getRepositoryTextUnits(repository))
                .extracting(TextUnitDTO::getName, TextUnitDTO::getTargetLocale, TextUnitDTO::getTarget)
                .containsExactly(
                        tuple("hello", "fr-FR", "Hello-fr-FR"),
                        tuple("bye", "fr-FR", "Bye-fr-FR"),
                        tuple("bye", "fr-FR", "Bye duplicate key-fr-FR"),
                        tuple("messageformat_1", "fr-FR", "{numCats, plural, one {{numCats,number} cat-fr-FR} other {{numCats,number} cats-fr-FR}}"),
                        tuple("messageformat_2", "fr-FR", "{numCats,plural,one{# cat}other{# cats}} and {numDogs,plural,one{# dog}other{# dogs}}-fr-FR"),
                        tuple("messageformat_3", "fr-FR", "{numCats, plural, one {Hello {name}! there is {numCats,number} cat-fr-FR} other {Hello {name}! There are {numCats,number} cats-fr-FR}}"),
                        tuple("hello", "ja-JP", "Hello-ja-JP"),
                        tuple("bye", "ja-JP", "Bye-ja-JP"),
                        tuple("bye", "ja-JP", "Bye duplicate key-ja-JP"),
                        tuple("messageformat_1", "ja-JP", "{numCats, plural, other {{numCats,number} cats-ja-JP}}"),
                        tuple("messageformat_2", "ja-JP", "{numCats,plural,one{# cat}other{# cats}} and {numDogs,plural,one{# dog}other{# dogs}}-ja-JP"),
                        tuple("messageformat_3", "ja-JP", "{numCats, plural, other {Hello {name}! There are {numCats,number} cats-ja-JP}}")
                );

        thirdPartyService.mapMojitoAndThirdPartyTextUnits(repository, testConfig.getProjectId(), null, Arrays.asList("json-sync=true"));
    }

    @Test
    public void testGetTranslatedUnits() {
        TextUnitDTO translatedTextUnitDto = new TextUnitDTO();
        translatedTextUnitDto.setTmTextUnitId(1L);
        translatedTextUnitDto.setAssetPath("assetPath");
        translatedTextUnitDto.setName("name-1");
        translatedTextUnitDto.setTarget("target-1");
        translatedTextUnitDto.setComment("comment-1");

        TextUnitDTO untranslatedTextUnitDto = new TextUnitDTO();
        untranslatedTextUnitDto.setTmTextUnitId(2L);
        untranslatedTextUnitDto.setAssetPath("assetPath");
        untranslatedTextUnitDto.setName("name-2");
        untranslatedTextUnitDto.setTarget("");
        untranslatedTextUnitDto.setComment("comment-2");

        TextUnitDTO untranslatedTextUnitDtoWithOriginalString = new TextUnitDTO();
        untranslatedTextUnitDtoWithOriginalString.setTmTextUnitId(2L);
        untranslatedTextUnitDtoWithOriginalString.setAssetPath("assetPath");
        untranslatedTextUnitDtoWithOriginalString.setName("name-2");
        untranslatedTextUnitDtoWithOriginalString.setTarget("target-2");
        untranslatedTextUnitDtoWithOriginalString.setComment("comment-2");

        ImmutableList<TextUnitDTO> textUnitDTOS = ImmutableList.of(translatedTextUnitDto, untranslatedTextUnitDto);
        ImmutableList<TextUnitDTO> textUnitDTOSWithOriginalStrings = ImmutableList.of(translatedTextUnitDto, untranslatedTextUnitDtoWithOriginalString);

        ThirdPartyTMSSmartlingWithJson thirdPartyTMSSmartlingWithJson = new ThirdPartyTMSSmartlingWithJson(null, null, null, null, null);

        ImmutableList<TextUnitDTO> result = thirdPartyTMSSmartlingWithJson.getTranslatedUnits(textUnitDTOS, textUnitDTOSWithOriginalStrings);

        // Only expecting the first text unit to be returned
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(translatedTextUnitDto.getTmTextUnitId(), result.get(0).getTmTextUnitId());
    }

    @Test
    public void testPullWithUntranslatedUnits() throws Exception {
        String repositoryName = testIdWatcher.getEntityName("repository");
        Repository repository = repositoryService.createRepository(repositoryName);
        RepositoryLocale repositoryLocaleFrFR = repositoryService.addRepositoryLocale(repository, "fr-FR");
        File smartlingFile = new File();
        String projectId = "testProject";
        String smartlingLocale = "fr-FR";
        ImmutableMap<String, String> localeMapping = ImmutableMap.of();

        String smartlingJsonResponse = "" +
                "{\n" +
                "  \"smartling\" : {\n" +
                "    \"translate_paths\" : {\n" +
                "      \"path\" : \"*/string\",\n" +
                "      \"instruction\" : \"*/note\",\n" +
                "      \"key\" : \"*/key\"\n" +
                "    },\n" +
                "    \"string_format\" : \"icu\",\n" +
                "    \"variants_enabled\" : \"true\"\n" +
                "  },\n" +
                "  \"strings\" : [\n" +
                "  {\n" +
                "    \"key\" : \"connectAccountButton\",\n" +
                "    \"tmTextUnitId\" : 1,\n" +
                "    \"assetPath\" : \"en.properties\",\n" +
                "    \"name\" : \"connectAccount.connectAccountButton\",\n" +
                "    \"string\" : \"Associer le compte\",\n" +
                "    \"note\" : \" connect account note\"\n" +
                "  },\n" +
                "\n" +
                "  {\n" +
                "    \"key\" : \"connectAccount.connectInstruction\",\n" +
                "    \"tmTextUnitId\" : 2,\n" +
                "    \"assetPath\" : \"en.properties\",\n" +
                "    \"name\" : \"connectAccount.connectInstruction\",\n" +
                // Untranslated string returned as an empty string
                "    \"string\" : \"\",\n" +
                "    \"note\" : \" connect instruction note\"\n" +
                "  }" +
                "]}";

        String smartlingJsonResponseWithOriginalString = "" +
                "{\n" +
                "  \"smartling\" : {\n" +
                "    \"translate_paths\" : {\n" +
                "      \"path\" : \"*/string\",\n" +
                "      \"instruction\" : \"*/note\",\n" +
                "      \"key\" : \"*/key\"\n" +
                "    },\n" +
                "    \"string_format\" : \"icu\",\n" +
                "    \"variants_enabled\" : \"true\"\n" +
                "  },\n" +
                "  \"strings\" : [\n" +
                "  {\n" +
                "    \"key\" : \"connectAccountButton\",\n" +
                "    \"tmTextUnitId\" : 1,\n" +
                "    \"assetPath\" : \"en.properties\",\n" +
                "    \"name\" : \"connectAccount.connectAccountButton\",\n" +
                "    \"string\" : \"Associer le compte\",\n" +
                "    \"note\" : \" connect account note\"\n" +
                "  },\n" +
                "\n" +
                "  {\n" +
                "    \"key\" : \"connectAccount.connectInstruction\",\n" +
                "    \"tmTextUnitId\" : 2,\n" +
                "    \"assetPath\" : \"en.properties\",\n" +
                "    \"name\" : \"connectAccount.connectInstruction\",\n" +
                // Untranslated string returned as the original string:
                "    \"string\" : \"Connect your account.\",\n" +
                "    \"note\" : \" connect instruction note\"\n" +
                "  }" +
                "]}";

        Mockito.doCallRealMethod()
                .when(thirdPartyTMSSmartlingWithJsonMock)
                .pull(repository, projectId, localeMapping);
        Mockito.when(thirdPartyTMSSmartlingWithJsonMock.hasEmptyTranslations(any()))
                .thenCallRealMethod();
        Mockito.when(thirdPartyTMSSmartlingWithJsonMock.getTranslatedUnits(any(), any()))
                .thenCallRealMethod();

        Mockito.when(thirdPartyTMSSmartlingWithJsonMock.getRepositoryFilesFromProject(repository, projectId))
                .thenReturn(ImmutableList.of(smartlingFile));
        Mockito.when(thirdPartyTMSSmartlingWithJsonMock.getRepositoryLocaleWithoutRootStream(repository))
                .thenReturn(Stream.of(repositoryLocaleFrFR));
        Mockito.when(thirdPartyTMSSmartlingWithJsonMock.getSmartlingLocale(localeMapping, repositoryLocaleFrFR))
                .thenReturn(smartlingLocale);

        thirdPartyTMSSmartlingWithJsonMock.smartlingJsonConverter = smartlingJsonConverter;
        thirdPartyTMSSmartlingWithJsonMock.textUnitBatchImporterService = textUnitBatchImporterServiceMock;

        Mockito.when(thirdPartyTMSSmartlingWithJsonMock.getLocalizedFileContent(projectId, smartlingFile, smartlingLocale, false))
                .thenReturn(smartlingJsonResponseWithOriginalString);

        // For the first pass, mock a fully translated response
        thirdPartyTMSSmartlingWithJsonMock.pull(repository, projectId, localeMapping);

        ArgumentCaptor<ImmutableList<TextUnitDTO>> dtoListCaptor = ArgumentCaptor.forClass(ImmutableList.class);
        Mockito.verify(textUnitBatchImporterServiceMock, times(1))
                .importTextUnits(dtoListCaptor.capture(), anyBoolean(), anyBoolean());
        ImmutableList<TextUnitDTO> translatedUnits = dtoListCaptor.getValue();

        // Expecting two fully translated units
        Assert.assertEquals(2, translatedUnits.size());


        // For the second pass, mock a partially translated response on the initial call
        Mockito.when(thirdPartyTMSSmartlingWithJsonMock.getRepositoryLocaleWithoutRootStream(repository))
                .thenReturn(Stream.of(repositoryLocaleFrFR));
        Mockito.when(thirdPartyTMSSmartlingWithJsonMock.getLocalizedFileContent(projectId, smartlingFile, smartlingLocale, false))
                .thenReturn(smartlingJsonResponse);
        Mockito.when(thirdPartyTMSSmartlingWithJsonMock.getLocalizedFileContent(projectId, smartlingFile, smartlingLocale, true))
                .thenReturn(smartlingJsonResponseWithOriginalString);

        thirdPartyTMSSmartlingWithJsonMock.pull(repository, projectId, localeMapping);

        dtoListCaptor = ArgumentCaptor.forClass(ImmutableList.class);
        Mockito.verify(textUnitBatchImporterServiceMock, times(2))
                .importTextUnits(dtoListCaptor.capture(), anyBoolean(), anyBoolean());
        translatedUnits = dtoListCaptor.getValue();

        // Expecting two fully translated units
        Assert.assertEquals(1, translatedUnits.size());

    }

    List<TextUnitDTO> getRepositoryTextUnits(Repository repository) {
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setRootLocaleExcluded(true);
        List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
        return search.stream()
                .peek(t -> logger.debug("name: \"{}\", locale: \"{}\", content: \"{}\")", t.getName(), t.getTargetLocale(), t.getTarget()))
                .sorted(Comparator.comparing(TextUnitDTO::getTargetLocale).thenComparing(TextUnitDTO::getTmTextUnitId))
                .collect(ImmutableList.toImmutableList());
    }
}