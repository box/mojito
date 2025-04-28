package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "l10n.glossary.cache.enabled", havingValue = "true")
public class GlossaryCacheService {

  private final GlossaryCacheBlobStorage glossaryCacheBlobStorage;

  private final GlossaryCacheBuilder glossaryCacheBuilder;

  private final StemmerService stemmerService;

  private GlossaryCache glossaryCache;

  public GlossaryCacheService(
      GlossaryCacheBlobStorage blobStorage,
      GlossaryCacheBuilder glossaryCacheBuilder,
      StemmerService stemmerService) {
    this.glossaryCacheBlobStorage = blobStorage;
    this.glossaryCacheBuilder = glossaryCacheBuilder;
    this.stemmerService = stemmerService;
    loadGlossaryCache();
  }

  /**
   * Get the list of glossary terms matches for the provided text.
   *
   * @param text
   * @return
   */
  public List<GlossaryTerm> getGlossaryTermsInText(String text) {
    if (glossaryCache.size() == 0) {
      // If the cache is empty, load it from blob storage
      loadGlossaryCache();
    }

    // TODO (mallen): Need to handle multiple matches, case sensitive, exact match checking here in
    // a follow-up PR
    return glossaryCache.get(stemmerService.stem(text));
  }

  public void buildGlossaryCache() {
    glossaryCacheBuilder.buildCache();
    loadGlossaryCache();
  }

  public void loadGlossaryCache() {
    glossaryCache = glossaryCacheBlobStorage.getGlossaryCache().orElseGet(GlossaryCache::new);
  }
}
