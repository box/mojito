package com.box.l10n.mojito.service.tm.importer;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ImporterCacheService {

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    AssetRepository assetRepository;

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
}