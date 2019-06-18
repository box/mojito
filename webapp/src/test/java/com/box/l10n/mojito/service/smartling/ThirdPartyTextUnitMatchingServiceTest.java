package com.box.l10n.mojito.service.smartling;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.ThirdPartyTextUnit;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.request.SourceStringsObject;
import com.box.l10n.mojito.smartling.request.StringData;
import com.box.l10n.mojito.smartling.request.StringInfo;
import com.box.l10n.mojito.smartling.response.BaseResponse;
import com.box.l10n.mojito.smartling.response.SourceStringsResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ThirdPartyTextUnitMatchingServiceTest extends ServiceTestBase {

    @InjectMocks
    ThirdPartyTextUnitMatchingService thirdPartyTextUnitMatchingService;

    @Mock
    ThirdPartyTextUnitRepository thirdPartyTextUnitRepository;

    @Mock
    SmartlingClient smartlingClientMock;

    @Mock
    ThirdPartyTextUnitSearchService thirdPartyTextUnitSearchService;

    private String projectId = "abc123";
    private String fileName = "repoName/12345_singular_source.xml";
    private int offset = 0;

    private TMTextUnit tmTextUnit = new TMTextUnit();

    private StringInfo stringInfo = new StringInfo();

    private Asset asset = new Asset();

    private ThirdPartyTextUnit thirdPartyTextUnit = new ThirdPartyTextUnit();

    private ThirdPartyTextUnitDTO thirdPartyTextUnitDTO = new ThirdPartyTextUnitDTO(
            1L, "thirdPartyTextUnitId", "assetPath#@#name",1L);

    private ThirdPartyTextUnitDTO thirdPartyTextUnitDTOShouldNotMatch = new ThirdPartyTextUnitDTO(
            2L, "", "", 2L);


    private ThirdPartyTextUnitForBatchImport thirdPartyTextUnitForBatchImport = new ThirdPartyTextUnitForBatchImport();
    @Before
    public void setUp() {
        tmTextUnit.setId(1L);

        thirdPartyTextUnitDTO.setAssetPath("assetPath");
        thirdPartyTextUnitDTO.setRepositoryName("repositoryName");
        thirdPartyTextUnitDTO.setTmTextUnitName("name");

        thirdPartyTextUnit.setThirdPartyTextUnitId(thirdPartyTextUnitDTO.getThirdPartyTextUnitId());
        thirdPartyTextUnit.setTmTextUnit(tmTextUnit);
        thirdPartyTextUnit.setMappingKey(
                thirdPartyTextUnitDTO.getAssetPath() + thirdPartyTextUnitMatchingService.delimiter + thirdPartyTextUnitDTO.getTmTextUnitName());

        stringInfo.setHashcode(thirdPartyTextUnit.getThirdPartyTextUnitId());
        stringInfo.setStringVariant(thirdPartyTextUnit.getMappingKey());

        thirdPartyTextUnitForBatchImport.setMappingKey(thirdPartyTextUnit.getMappingKey());
        thirdPartyTextUnitForBatchImport.setThirdPartyTextUnitId(thirdPartyTextUnit.getThirdPartyTextUnitId());
    }

    @Test
    public void processFile() {
        SourceStringsResponse sourceStringsResponse = new SourceStringsResponse();
        SourceStringsObject sourceStringsObject = new SourceStringsObject();
        StringData stringData = new StringData();
        stringData.setItems(Collections.singletonList(stringInfo));
        sourceStringsObject.setData(stringData);
        sourceStringsObject.setCode(BaseResponse.API_SUCCESS_CODE);
        sourceStringsResponse.setResponse(sourceStringsObject);
        when(smartlingClientMock.getSourceStrings(projectId, fileName, offset)).thenReturn(sourceStringsResponse);

        thirdPartyTextUnitForBatchImport.setAsset(asset);
        when(thirdPartyTextUnitSearchService.convertDTOToBatchImport(Matchers.any(Set.class), Matchers.anyBoolean()))
                .thenReturn(Collections.singletonList(thirdPartyTextUnitForBatchImport));

        when(thirdPartyTextUnitSearchService.getByThirdPartyTextUnitIdsAndMappingKeys(Collections.singletonList(thirdPartyTextUnitForBatchImport)))
                .thenReturn(Collections.singletonList(thirdPartyTextUnitDTO));

        thirdPartyTextUnitMatchingService.processFile(fileName, projectId);

        Mockito.verify(smartlingClientMock).getSourceStrings(projectId, fileName, offset);
        Mockito.verify(thirdPartyTextUnitSearchService).convertDTOToBatchImport(Matchers.any(Set.class), Matchers.anyBoolean());
        Mockito.verify(thirdPartyTextUnitSearchService)
                .getByThirdPartyTextUnitIdsAndMappingKeys(Collections.singletonList(thirdPartyTextUnitForBatchImport));
        Mockito.verify(thirdPartyTextUnitRepository, never()).findByThirdPartyTextUnitId(thirdPartyTextUnitDTO.getThirdPartyTextUnitId());
    }

    @Test
    public void importThirdPartyTextUnitOfAssetNoCurrentThirdPartyTextUnit() {
        thirdPartyTextUnitForBatchImport.setTmTextUnit(tmTextUnit);

        when(thirdPartyTextUnitRepository.findByThirdPartyTextUnitId(thirdPartyTextUnitDTO.getThirdPartyTextUnitId()))
                .thenReturn(thirdPartyTextUnit);

        thirdPartyTextUnitMatchingService.importThirdPartyTextUnitOfAsset(Collections.singletonList(thirdPartyTextUnitForBatchImport));

        Mockito.verify(thirdPartyTextUnitRepository).findByThirdPartyTextUnitId(thirdPartyTextUnitDTO.getThirdPartyTextUnitId());
    }

    @Test
    public void importThirdPartyTextUnitOfAsset() {
        thirdPartyTextUnitForBatchImport.setCurrentThirdPartyTextUnitDTO(thirdPartyTextUnitDTO);
        thirdPartyTextUnitMatchingService.importThirdPartyTextUnitOfAsset(Collections.singletonList(thirdPartyTextUnitForBatchImport));

        Mockito.verify(thirdPartyTextUnitRepository, never()).findByThirdPartyTextUnitId(thirdPartyTextUnitDTO.getThirdPartyTextUnitId());
    }

    @Test
    public void mapThirdPartyTextUnitsToImportWithExistingShouldNotMatch() {
        when(thirdPartyTextUnitSearchService.getByThirdPartyTextUnitIdsAndMappingKeys(Collections.singletonList(thirdPartyTextUnitForBatchImport)))
                .thenReturn(Collections.singletonList(thirdPartyTextUnitDTOShouldNotMatch));

        thirdPartyTextUnitMatchingService.mapThirdPartyTextUnitsToImportWithExisting(
                asset, Collections.singletonList(thirdPartyTextUnitForBatchImport));

        assertNull(thirdPartyTextUnitForBatchImport.currentThirdPartyTextUnitDTO);
        Mockito.verify(thirdPartyTextUnitSearchService)
                .getByThirdPartyTextUnitIdsAndMappingKeys(Collections.singletonList(thirdPartyTextUnitForBatchImport));
    }

    @Test
    public void mapThirdPartyTextUnitsToImportWithExistingNoResultsFromRepo() {
        when(thirdPartyTextUnitSearchService.getByThirdPartyTextUnitIdsAndMappingKeys(Collections.singletonList(thirdPartyTextUnitForBatchImport)))
                .thenReturn(Collections.emptyList());

        thirdPartyTextUnitMatchingService.mapThirdPartyTextUnitsToImportWithExisting(
                asset, Collections.singletonList(thirdPartyTextUnitForBatchImport));

        assertNull(thirdPartyTextUnitForBatchImport.currentThirdPartyTextUnitDTO);
        Mockito.verify(thirdPartyTextUnitSearchService)
                .getByThirdPartyTextUnitIdsAndMappingKeys(Collections.singletonList(thirdPartyTextUnitForBatchImport));
    }

    @Test
    public void mapThirdPartyTextUnitsToImportWithExisting() {
        when(thirdPartyTextUnitSearchService.getByThirdPartyTextUnitIdsAndMappingKeys(Collections.singletonList(thirdPartyTextUnitForBatchImport)))
                .thenReturn(Collections.singletonList(thirdPartyTextUnitDTO));

        thirdPartyTextUnitMatchingService.mapThirdPartyTextUnitsToImportWithExisting(
                asset, Collections.singletonList(thirdPartyTextUnitForBatchImport));

        assertNotNull(thirdPartyTextUnitForBatchImport.currentThirdPartyTextUnitDTO);
        Mockito.verify(thirdPartyTextUnitSearchService)
                .getByThirdPartyTextUnitIdsAndMappingKeys(Collections.singletonList(thirdPartyTextUnitForBatchImport));
    }

    @Test
    public void shouldNotConvertStringInfoMissingDelimiterToDTO() {
        StringInfo stringInfoMissingDelimiter = new StringInfo();
        stringInfoMissingDelimiter.setStringVariant("");
        Set<ThirdPartyTextUnitDTO> thirdPartyTextUnitDTOSet = thirdPartyTextUnitMatchingService
                .convertStringInfoToDTO(Collections.singletonList(stringInfoMissingDelimiter), fileName);

        assertTrue(thirdPartyTextUnitDTOSet.isEmpty());
    }

    @Test
    public void convertStringInfoToDTO() {
        Set<ThirdPartyTextUnitDTO> thirdPartyTextUnitDTOSet = thirdPartyTextUnitMatchingService
                .convertStringInfoToDTO(Collections.singletonList(stringInfo), fileName);

        assertFalse(thirdPartyTextUnitDTOSet.isEmpty());
    }

    @Test
    public void convertStringInfoToDTOShouldDedupe() {
        Set<ThirdPartyTextUnitDTO> thirdPartyTextUnitDTOSet = thirdPartyTextUnitMatchingService
                .convertStringInfoToDTO(Arrays.asList(stringInfo, stringInfo), fileName);

        assertEquals(1, thirdPartyTextUnitDTOSet.size());
    }

    @Test
    public void match() {
        Function<ThirdPartyTextUnitForBatchImport, Optional<ThirdPartyTextUnitDTO>> matches = thirdPartyTextUnitMatchingService
                .match(Collections.singletonList(thirdPartyTextUnitDTO));
        assertTrue(matches.apply(thirdPartyTextUnitForBatchImport).isPresent());
    }

    @Test
    public void shouldNotMatch() {
        Function<ThirdPartyTextUnitForBatchImport, Optional<ThirdPartyTextUnitDTO>> matches = thirdPartyTextUnitMatchingService
                .match(Collections.singletonList(thirdPartyTextUnitDTOShouldNotMatch));
        assertFalse(matches.apply(thirdPartyTextUnitForBatchImport).isPresent());
    }

    @Test
    public void createMatchByThirdPartyTextIdAndMappingKey() {
        Function<ThirdPartyTextUnitForBatchImport, Optional<ThirdPartyTextUnitDTO>> matches = thirdPartyTextUnitMatchingService
                .createMatchByThirdPartyTextIdAndMappingKey(Collections.singletonList(thirdPartyTextUnitDTO));
        assertTrue(matches.apply(thirdPartyTextUnitForBatchImport).isPresent());
    }

    @Test
    public void createMatchByThirdPartyTextIdAndMappingKeyShouldNotMatch() {
        Function<ThirdPartyTextUnitForBatchImport, Optional<ThirdPartyTextUnitDTO>> matches = thirdPartyTextUnitMatchingService
                .createMatchByThirdPartyTextIdAndMappingKey(Collections.singletonList(thirdPartyTextUnitDTOShouldNotMatch));
        assertFalse(matches.apply(thirdPartyTextUnitForBatchImport).isPresent());
    }

    @Test
    public void updateShouldBeNeeded() {
        boolean isUpdateNeeded = thirdPartyTextUnitMatchingService.isUpdateNeededForThirdPartyTextUnit(thirdPartyTextUnitForBatchImport);
        assertTrue(isUpdateNeeded);
    }

    @Test
    public void updateShouldNotBeNeeded(){
        thirdPartyTextUnitForBatchImport.setTmTextUnit(tmTextUnit);
        thirdPartyTextUnitForBatchImport.setCurrentThirdPartyTextUnitDTO(thirdPartyTextUnitDTO);
        boolean isUpdateNeeded = thirdPartyTextUnitMatchingService.isUpdateNeededForThirdPartyTextUnit(thirdPartyTextUnitForBatchImport);
        assertFalse(isUpdateNeeded);
    }

}