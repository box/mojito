package com.box.l10n.mojito.service.tm.importer;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ImporterCacheService {

    @Autowired
    AssetRepository assetRepository;

    public LoadingCache<Map.Entry<String, String>, Asset> createAssetsCache() {
        LoadingCache<Map.Entry<String, String>, Asset> assetsCache = CacheBuilder.newBuilder().build(
                CacheLoader.from((entry) -> assetRepository.findByPathAndRepositoryName(entry.getKey(), entry.getValue()))
        );
        return assetsCache;
    }
}
