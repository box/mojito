package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.VirtualAsset;
import com.box.l10n.mojito.service.asset.VirtualAssetBadRequestException;
import com.box.l10n.mojito.service.asset.VirtualAssetRequiredException;
import com.box.l10n.mojito.service.asset.VirtualAssetService;
import com.box.l10n.mojito.service.asset.VirtualAssetTextUnit;
import com.box.l10n.mojito.service.asset.VirutalAssetMissingTextUnitException;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.SmartlingClientException;
import com.box.l10n.mojito.smartling.response.GlossaryDetails;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThirdPartyTMSSmartlingGlossaryTest {

    private static final String SCHOOL_TEXT_UNIT_ID = "bd3a096a-ab4f-49ac-b033-099086cfe271";
    public static final String HOUSE_TEXT_UNIT_ID = "8300f245-5072-4918-a59b-4d1cdf5c8cf2";
    public static final String FIELD_TEXT_UNIT_ID = "b35b5435-16ca-4fa3-95cd-8527420c9240";
    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Mock
    VirtualAssetService mockVirtualAssetService;

    @Mock
    SmartlingClient mockSmartlingClient;

    @Mock
    AssetRepository mockAssetRepository;

    @Mock
    AssetTextUnitRepository mockAssetTextUnitRepository;

    @Mock
    Repository mockRepository;

    @Mock
    Asset mockAsset;

    @Mock
    AssetExtraction mockAssetExtraction;

    @Captor
    ArgumentCaptor<List<VirtualAssetTextUnit>> textUnitsCaptor;

    @Captor
    ArgumentCaptor<Long> localeIdCaptor;

    @Captor
    ArgumentCaptor<Long> assetIdCaptor;

    ThirdPartyTMSSmartlingGlossary thirdPartyTMSSmartlingGlossary;

    String testTBXFileString;

    Map<String, String> localeMapping = Maps.newHashMap();

    @Before
    public void setup() throws IOException, VirtualAssetBadRequestException {
        MockitoAnnotations.initMocks(this);
        VirtualAsset virtualAsset = new VirtualAsset();
        virtualAsset.setId(100L);
        virtualAsset.setPath("Test Glossary");
        GlossaryDetails glossaryDetails = new GlossaryDetails();
        glossaryDetails.setName("Test Glossary");
        StringBuilder tbxStringBuilder = new StringBuilder();
        Files.readLines(new File("src/test/resources/com/box/l10n/mojito/service/thirdparty/Test_Glossary.tbx"), StandardCharsets.UTF_8).stream().forEach(line -> {
            tbxStringBuilder.append(line);
        });
        localeMapping.put("en", "en-US");
        testTBXFileString = tbxStringBuilder.toString();
        thirdPartyTMSSmartlingGlossary = new ThirdPartyTMSSmartlingGlossary();
        thirdPartyTMSSmartlingGlossary.smartlingClient = mockSmartlingClient;
        thirdPartyTMSSmartlingGlossary.assetTextUnitRepository = mockAssetTextUnitRepository;
        thirdPartyTMSSmartlingGlossary.assetRepository = mockAssetRepository;
        thirdPartyTMSSmartlingGlossary.virtualAssetService = mockVirtualAssetService;
        thirdPartyTMSSmartlingGlossary.accountId = "testAccountId";

        AssetTextUnit school = new AssetTextUnit();
        school.setName("school");
        school.setId(1L);

        AssetTextUnit house = new AssetTextUnit();
        house.setName("house");
        house.setId(2L);

        List<AssetTextUnit> assetTextUnitList = Lists.newArrayList(school, house);

        RepositoryLocale en = new RepositoryLocale();
        Locale enLocale = new Locale();
        enLocale.setBcp47Tag("en");
        enLocale.setId(1L);
        en.setLocale(enLocale);
        RepositoryLocale frFR = new RepositoryLocale();
        Locale frFRLocale = new Locale();
        frFR.setParentLocale(en);
        frFRLocale.setBcp47Tag("fr-FR");
        frFRLocale.setId(2L);
        frFR.setLocale(frFRLocale);
        when(mockRepository.getRepositoryLocales()).thenReturn(Sets.newHashSet(en, frFR));
        doReturn(glossaryDetails).when(mockSmartlingClient).getGlossaryDetails(anyString(), anyString());
        doReturn(testTBXFileString).when(mockSmartlingClient).downloadGlossaryFile(anyString(), anyString());
        doReturn(testTBXFileString).when(mockSmartlingClient).downloadSourceGlossaryFile(anyString(), anyString(), anyString());
        doReturn(testTBXFileString).when(mockSmartlingClient).downloadGlossaryFileWithTranslations(anyString(), anyString(), anyString(), anyString());
        doReturn(virtualAsset).when(mockVirtualAssetService).createOrUpdateVirtualAsset(isA(VirtualAsset.class));
        doReturn(Optional.of(mockAsset)).when(mockAssetRepository).findById(100L);
        when(mockAsset.getLastSuccessfulAssetExtraction()).thenReturn(mockAssetExtraction);
        when(mockAsset.getId()).thenReturn(100L);
        when(mockAssetExtraction.getId()).thenReturn(1L);
        doReturn(assetTextUnitList).when(mockAssetTextUnitRepository).findByAssetExtractionId(anyLong());
    }

    @Test
    public void testPullSourceDeletesExistingAndImportsTextUnits() throws VirtualAssetRequiredException {
        thirdPartyTMSSmartlingGlossary.pullSourceTextUnits(mockRepository, "someUid", localeMapping);
        verify(mockSmartlingClient, times(1)).getGlossaryDetails("testAccountId", "someUid");
        verify(mockSmartlingClient, times(1)).downloadSourceGlossaryFile("testAccountId", "someUid", "en-US");
        verify(mockVirtualAssetService, times(1)).deleteTextUnit(100L, "school");
        verify(mockVirtualAssetService, times(1)).deleteTextUnit(100L, "house");
        verify(mockVirtualAssetService, times(1)).addTextUnits(anyLong(), textUnitsCaptor.capture());
        List<VirtualAssetTextUnit> virtualAssetTextUnits = textUnitsCaptor.getValue();
        assertEquals(3, virtualAssetTextUnits.size());
        assertTrue(virtualAssetTextUnits.stream().filter(textUnit -> textUnit.getContent().equals("school") && textUnit.getName().equals(SCHOOL_TEXT_UNIT_ID)).count() == 1);
        assertTrue(virtualAssetTextUnits.stream().filter(textUnit -> textUnit.getContent().equals("house") && textUnit.getName().equals(HOUSE_TEXT_UNIT_ID)).count() == 1);
        assertTrue(virtualAssetTextUnits.stream().filter(textUnit -> textUnit.getContent().equals("field") && textUnit.getName().equals(FIELD_TEXT_UNIT_ID)).count() == 1);
        assertTrue(virtualAssetTextUnits.stream().filter(virtualAssetTextUnit -> virtualAssetTextUnit.getComment().equals("school comment")).count() == 1);
        assertTrue(virtualAssetTextUnits.stream().filter(virtualAssetTextUnit -> virtualAssetTextUnit.getComment().equals("house comment")).count() == 1);
        assertTrue(virtualAssetTextUnits.stream().filter(virtualAssetTextUnit -> virtualAssetTextUnit.getComment().equals("field comment --- POS: noun")).count() == 1);
    }

    @Test(expected = SmartlingClientException.class)
    public void testExceptionThrownIfErrorRetrievingGlossaryDetails() {
        doThrow(new RuntimeException("External Smartling error")).when(mockSmartlingClient).getGlossaryDetails(anyString(), anyString());
        thirdPartyTMSSmartlingGlossary.pullSourceTextUnits(mockRepository, "someUid", localeMapping);
    }

    @Test(expected = SmartlingClientException.class)
    public void testExceptionThrownIfErrorDownloadingSourceGlossaryFile() {
        doThrow(new RuntimeException("External Smartling error")).when(mockSmartlingClient).downloadSourceGlossaryFile(anyString(), anyString(), anyString());
        thirdPartyTMSSmartlingGlossary.pullSourceTextUnits(mockRepository, "someUid", localeMapping);
    }

    @Test
    public void testPull() throws VirutalAssetMissingTextUnitException, VirtualAssetRequiredException {
        thirdPartyTMSSmartlingGlossary.pull(mockRepository, "someUid", localeMapping);
        verify(mockSmartlingClient, times(1)).downloadGlossaryFileWithTranslations("testAccountId", "someUid", "fr-FR", "en-US");
        verify(mockVirtualAssetService, times(1)).importLocalizedTextUnits(assetIdCaptor.capture(), localeIdCaptor.capture(), textUnitsCaptor.capture());
        List<VirtualAssetTextUnit> translatedTextUnits = textUnitsCaptor.getValue();
        assertTrue(assetIdCaptor.getValue() == 100L);
        assertTrue(localeIdCaptor.getValue() == 2L);
        assertEquals(3, translatedTextUnits.size());
        assertTrue(translatedTextUnits.stream().filter(textUnit -> textUnit.getContent().equals("l'école") && textUnit.getName().equals(SCHOOL_TEXT_UNIT_ID) ).count() == 1);
        assertTrue(translatedTextUnits.stream().filter(textUnit -> textUnit.getContent().equals("maison") && textUnit.getName().equals(HOUSE_TEXT_UNIT_ID)).count() == 1);
        assertTrue(translatedTextUnits.stream().filter(textUnit -> textUnit.getContent().equals("le champ") && textUnit.getName().equals(FIELD_TEXT_UNIT_ID)).count() == 1);
    }

    @Test
    public void testGetThirdPartyTextUnits() {
        List<ThirdPartyTextUnit> thirdPartyTextUnits = thirdPartyTMSSmartlingGlossary.getThirdPartyTextUnits("someUid");
        verify(mockSmartlingClient, times(1)).getGlossaryDetails("testAccountId", "someUid");
        verify(mockSmartlingClient, times(1)).downloadGlossaryFile("testAccountId", "someUid");
        assertEquals(3, thirdPartyTextUnits.size());
        assertTrue(thirdPartyTextUnits.stream().filter(textUnit -> textUnit.getId().equals(SCHOOL_TEXT_UNIT_ID) && textUnit.getName().equals(SCHOOL_TEXT_UNIT_ID)).count() == 1);
        assertTrue(thirdPartyTextUnits.stream().filter(textUnit -> textUnit.getId().equals(HOUSE_TEXT_UNIT_ID) && textUnit.getName().equals(HOUSE_TEXT_UNIT_ID)).count() == 1);
        assertTrue(thirdPartyTextUnits.stream().filter(textUnit -> textUnit.getId().equals(FIELD_TEXT_UNIT_ID) && textUnit.getName().equals(FIELD_TEXT_UNIT_ID)).count() == 1);
        assertEquals(3, thirdPartyTextUnits.stream().filter(textUnit -> textUnit.getAssetPath().equals("Test Glossary")).count());
    }

    @Test(expected = SmartlingClientException.class)
    public void testExceptionThrownIfErrorDownloadingGlossaryFile() {
        doThrow(new RuntimeException("External Smartling error")).when(mockSmartlingClient).downloadGlossaryFile(anyString(), anyString());
        thirdPartyTMSSmartlingGlossary.getThirdPartyTextUnits("someUid");
    }

    @Test(expected = SmartlingClientException.class)
    public void testExceptionThrownIfErrorDownloadingTranslatedGlossaryFile() {
        doThrow(new RuntimeException("External Smartling error")).when(mockSmartlingClient).downloadGlossaryFileWithTranslations(anyString(), anyString(), anyString(), anyString());
        thirdPartyTMSSmartlingGlossary.pull(mockRepository, "someUid", localeMapping);
    }
}
