package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "l10n.glossary.cache.enabled", havingValue = "true")
public class GlossaryCacheBlobStorage {

  private final StructuredBlobStorage structuredBlobStorage;
  private final String glossaryCacheName = "smartling-glossary-cache";

  ObjectMapper objectMapper;

  public GlossaryCacheBlobStorage(
      ObjectMapper objectMapper, StructuredBlobStorage structuredBlobStorage) {

    this.objectMapper = objectMapper;
    this.structuredBlobStorage = structuredBlobStorage;
  }

  public void putGlossaryCache(GlossaryCache glossaryCache) {
    structuredBlobStorage.put(
        StructuredBlobStorage.Prefix.GLOSSARY,
        glossaryCacheName,
        objectMapper.writeValueAsStringUnchecked(glossaryCache),
        Retention.PERMANENT);
  }

  public Optional<GlossaryCache> getGlossaryCache() {
    Optional<String> glossaryJson =
        structuredBlobStorage.getString(StructuredBlobStorage.Prefix.GLOSSARY, glossaryCacheName);
    return glossaryJson.map(s -> objectMapper.readValueUnchecked(s, GlossaryCache.class));
  }
}
