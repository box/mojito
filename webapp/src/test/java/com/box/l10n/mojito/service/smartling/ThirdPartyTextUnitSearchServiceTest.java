package com.box.l10n.mojito.service.smartling;

import com.box.l10n.mojito.entity.*;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.importer.ImporterCacheService;
import com.google.common.cache.LoadingCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ThirdPartyTextUnitSearchServiceTest {

    @InjectMocks
    ThirdPartyTextUnitSearchService thirdPartyTextUnitSearchService;

    @Mock
    ImporterCacheService importerCacheService;

    @Mock
    ThirdPartyTextUnitRepository thirdPartyTextUnitRepository;

    @Mock
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    private TMTextUnit tmTextUnit = new TMTextUnit();

    private ThirdPartyTextUnit thirdPartyTextUnit = new ThirdPartyTextUnit();

    private ThirdPartyTextUnitDTO thirdPartyTextUnitDTO = new ThirdPartyTextUnitDTO(
            null,
            "thirdPartyTextUnitId",
            "mappingKey",
            null
    );

    private ThirdPartyTextUnitForBatchImport thirdPartyTextUnitForBatchImport = new ThirdPartyTextUnitForBatchImport();

    @Before
    public void setUp() throws Exception {
        tmTextUnit.setId(1L);
        thirdPartyTextUnit.setMappingKey("mappingKey");
        thirdPartyTextUnit.setThirdPartyTextUnitId("thirdPartyTextUnitId");
        thirdPartyTextUnit.setTmTextUnit(tmTextUnit);
        thirdPartyTextUnitDTO.setAssetPath("assetPath");
        thirdPartyTextUnitDTO.setRepositoryName("repositoryName");
        thirdPartyTextUnitDTO.setTmTextUnitName("name");
        thirdPartyTextUnitForBatchImport.setMappingKey(thirdPartyTextUnit.getMappingKey());
        thirdPartyTextUnitForBatchImport.setThirdPartyTextUnitId(thirdPartyTextUnit.getThirdPartyTextUnitId());
    }

    @Test
    public void convertDTOToBatchImport() {
        LoadingCache<Map.Entry<String, String>, Asset> assetCache = Mockito.mock(LoadingCache.class);

        Repository repository = new Repository();
        repository.setName(thirdPartyTextUnitDTO.getRepositoryName());
        Asset asset = new Asset();
        asset.setId(1L);
        asset.setRepository(repository);
        TMTextUnitCurrentVariant tmTextUnitCurrentVariant = new TMTextUnitCurrentVariant();
        tmTextUnitCurrentVariant.setTmTextUnit(tmTextUnit);

        when(assetCache.getUnchecked(new AbstractMap.SimpleEntry<>(thirdPartyTextUnitDTO.getAssetPath(), repository.getName()))).thenReturn(asset);

        when(importerCacheService.createAssetsCache()).thenReturn(assetCache);
        when(tmTextUnitCurrentVariantRepository.findAllByTmTextUnit_NameAndTmTextUnit_Asset_Id(thirdPartyTextUnitDTO.getTmTextUnitName(), asset.getId()))
            .thenReturn(Collections.singletonList(tmTextUnitCurrentVariant));

        Set<ThirdPartyTextUnitDTO> thirdPartyTextUnitDTOSet = new HashSet<>();
        thirdPartyTextUnitDTOSet.add(thirdPartyTextUnitDTO);

        List<ThirdPartyTextUnitForBatchImport> thirdPartyTextUnitForBatchImportList = thirdPartyTextUnitSearchService.convertDTOToBatchImport(
                thirdPartyTextUnitDTOSet, false
        );

        assertEquals(thirdPartyTextUnitForBatchImportList.get(0).getAsset(), asset);
        assertEquals(thirdPartyTextUnitForBatchImportList.get(0).getRepository(), repository);
        assertEquals(thirdPartyTextUnitForBatchImportList.get(0).getTmTextUnit(), tmTextUnit);
    }

    @Test
    public void convertDTOToBatchImportEmptySet() {
        Set<ThirdPartyTextUnitDTO> thirdPartyTextUnitDTOSet = new HashSet<>();
        List<ThirdPartyTextUnitForBatchImport> thirdPartyTextUnitForBatchImportList = thirdPartyTextUnitSearchService.convertDTOToBatchImport(
                thirdPartyTextUnitDTOSet, false
        );

        assertTrue(thirdPartyTextUnitForBatchImportList.isEmpty());
    }

    @Test
    public void convertDTOToBatchImportNoTextUnitMatch() {
        LoadingCache<String, Repository> repositoryCache = Mockito.mock(LoadingCache.class);
        LoadingCache<Map.Entry<String, String>, Asset> assetCache = Mockito.mock(LoadingCache.class);

        Repository repository = new Repository();
        repository.setName(thirdPartyTextUnitDTO.getRepositoryName());
        Asset asset = new Asset();
        asset.setRepository(repository);
        asset.setId(1L);

        when(repositoryCache.getUnchecked(thirdPartyTextUnitDTO.getRepositoryName())).thenReturn(repository);
        when(assetCache.getUnchecked(new AbstractMap.SimpleEntry<>(thirdPartyTextUnitDTO.getAssetPath(), repository.getName()))).thenReturn(asset);

        when(importerCacheService.createAssetsCache()).thenReturn(assetCache);
        when(tmTextUnitCurrentVariantRepository.findAllByTmTextUnit_NameAndTmTextUnit_Asset_Id(thirdPartyTextUnitDTO.getTmTextUnitName(), asset.getId()))
                .thenReturn(Collections.emptyList());

        Set<ThirdPartyTextUnitDTO> thirdPartyTextUnitDTOSet = new HashSet<>();
        thirdPartyTextUnitDTOSet.add(thirdPartyTextUnitDTO);

        List<ThirdPartyTextUnitForBatchImport> thirdPartyTextUnitForBatchImportList = thirdPartyTextUnitSearchService.convertDTOToBatchImport(
                thirdPartyTextUnitDTOSet, false
        );

        assertTrue(thirdPartyTextUnitForBatchImportList.isEmpty());
    }

    @Test
    public void getByThirdPartyTextUnitIdsAndMappingKeys() {
        when(thirdPartyTextUnitRepository.getByThirdPartyTextUnitIdIsInAndMappingKeyIsIn(
                Collections.singletonList(thirdPartyTextUnit.getThirdPartyTextUnitId()),
                Collections.singletonList(thirdPartyTextUnit.getMappingKey())
        )).thenReturn(Collections.singletonList(thirdPartyTextUnitDTO));

        List<ThirdPartyTextUnitDTO> result = thirdPartyTextUnitSearchService.getByThirdPartyTextUnitIdsAndMappingKeys(
                Collections.singletonList(thirdPartyTextUnitForBatchImport));

        assertEquals(result.get(0).thirdPartyTextUnitId,
                thirdPartyTextUnit.getThirdPartyTextUnitId());
        assertEquals(result.get(0).mappingKey,
                thirdPartyTextUnit.getMappingKey());

    }

    @Test
    public void getByThirdPartyTextUnitIdsAndMappingKeysPassedEmptyList() {
        when(thirdPartyTextUnitRepository.getByThirdPartyTextUnitIdIsInAndMappingKeyIsIn(
                Collections.emptyList(),
                Collections.emptyList()
        )).thenReturn(Collections.emptyList());

        List<ThirdPartyTextUnitDTO> result = thirdPartyTextUnitSearchService.getByThirdPartyTextUnitIdsAndMappingKeys(
                Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    public void getByThirdPartyTextUnitIdsAndMappingKeysNoMatchInRepo() {
        when(thirdPartyTextUnitRepository.getByThirdPartyTextUnitIdIsInAndMappingKeyIsIn(
                Collections.singletonList(thirdPartyTextUnit.getThirdPartyTextUnitId()),
                Collections.singletonList(thirdPartyTextUnit.getMappingKey())
        )).thenReturn(Collections.emptyList());

        List<ThirdPartyTextUnitDTO> result = thirdPartyTextUnitSearchService.getByThirdPartyTextUnitIdsAndMappingKeys(
                Collections.singletonList(thirdPartyTextUnitForBatchImport));

        assertTrue(result.isEmpty());
    }

    @Test
    public void search() {
        when(thirdPartyTextUnitRepository.getByThirdPartyTextUnitIdIsInAndMappingKeyIsIn(
                Collections.singletonList(thirdPartyTextUnit.getThirdPartyTextUnitId()),
                Collections.singletonList(thirdPartyTextUnit.getMappingKey())
        )).thenReturn(Collections.singletonList(thirdPartyTextUnitDTO));

        List<ThirdPartyTextUnitDTO> result = thirdPartyTextUnitSearchService.search(
                Collections.singletonList(thirdPartyTextUnit.getThirdPartyTextUnitId()),
                Collections.singletonList(thirdPartyTextUnit.getMappingKey()));

        assertEquals(result.get(0).thirdPartyTextUnitId,
                thirdPartyTextUnit.getThirdPartyTextUnitId());
        assertEquals(result.get(0).mappingKey,
                thirdPartyTextUnit.getMappingKey());
    }

    @Test
    public void searchNoMatchInRepo() {
        when(thirdPartyTextUnitRepository.getByThirdPartyTextUnitIdIsInAndMappingKeyIsIn(
                Collections.singletonList(thirdPartyTextUnit.getThirdPartyTextUnitId()),
                Collections.singletonList(thirdPartyTextUnit.getMappingKey())
        )).thenReturn(Collections.emptyList());

        List<ThirdPartyTextUnitDTO> result = thirdPartyTextUnitSearchService.search(
                Collections.singletonList(thirdPartyTextUnit.getThirdPartyTextUnitId()),
                Collections.singletonList(thirdPartyTextUnit.getMappingKey()));

        assertTrue(result.isEmpty());
    }

    @Test
    public void searchEmptyList() {
        when(thirdPartyTextUnitRepository.getByThirdPartyTextUnitIdIsInAndMappingKeyIsIn(
                Collections.emptyList(),
                Collections.emptyList()
        )).thenReturn(Collections.emptyList());

        List<ThirdPartyTextUnitDTO> result = thirdPartyTextUnitSearchService.search(
                Collections.emptyList(),
                Collections.emptyList());

        assertTrue(result.isEmpty());
    }
}