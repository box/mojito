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
        LoadingCache<String, Repository> repositoryCache = Mockito.mock(LoadingCache.class);
        LoadingCache<Map.Entry<String, Long>, Asset> assetCache = Mockito.mock(LoadingCache.class);

        Repository repository = new Repository();
        repository.setId(1L);
        Asset asset = new Asset();
        asset.setId(1L);
        TMTextUnitCurrentVariant tmTextUnitCurrentVariant = new TMTextUnitCurrentVariant();
        tmTextUnitCurrentVariant.setTmTextUnit(tmTextUnit);

        when(repositoryCache.getUnchecked(thirdPartyTextUnitDTO.getRepositoryName())).thenReturn(repository);
        when(assetCache.getUnchecked(new AbstractMap.SimpleEntry<>(thirdPartyTextUnitDTO.getAssetPath(), repository.getId()))).thenReturn(asset);

        when(importerCacheService.createRepositoriesCache()).thenReturn(repositoryCache);
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
}