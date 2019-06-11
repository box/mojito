package com.box.l10n.mojito.service.tm.importer;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ImporterCacheService {

    private final RepositoryRepository repositoryRepository;

    private final AssetRepository assetRepository;

    private final TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    @Autowired
    public ImporterCacheService(RepositoryRepository repositoryRepository,
                                AssetRepository assetRepository,
                                TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository) {
        this.repositoryRepository = repositoryRepository;
        this.assetRepository = assetRepository;
        this.tmTextUnitCurrentVariantRepository = tmTextUnitCurrentVariantRepository;

    }

    public LoadingCache<String, Repository> createRepositoriesCache() {
        LoadingCache<String, Repository> repositoriesCache = CacheBuilder.newBuilder().build(
                CacheLoader.from((name) -> repositoryRepository.findByName(name))
        );
        return repositoriesCache;
    }

    public LoadingCache<Map.Entry<String, Long>, Asset> createAssetsCache() {
        LoadingCache<Map.Entry<String, Long>, Asset> assetsCache = CacheBuilder.newBuilder().build(
                CacheLoader.from((entry) -> assetRepository.findByPathAndRepositoryId(entry.getKey(), entry.getValue()))
        );
        return assetsCache;
    }

    public LoadingCache<Map.Entry<String, Long>, TMTextUnitCurrentVariant> createTmTextUnitCurrentVariantCache() {
        LoadingCache<Map.Entry<String, Long>, TMTextUnitCurrentVariant> tmTextUnitCurrentVariantCache = CacheBuilder.newBuilder().build(
                CacheLoader.from((entry) -> tmTextUnitCurrentVariantRepository.findByTmTextUnit_NameAndTmTextUnit_Asset_Id(
                        entry.getKey(), entry.getValue()
                )));
        return tmTextUnitCurrentVariantCache;
    }
}
