package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    List<String> tokens = List.of(stemmerService.stem(text).split(" "));

    List<Match> matches = findGlossaryMatches(tokens, text);
    matches = resolveOverlaps(matches);

    matches.removeIf(
        match ->
            (match.term.isCaseSensitive() && !text.contains(match.term.getText()))
                || (match.term.isExactMatch()
                    && !text.toLowerCase().contains(match.term.getText().toLowerCase())));

    return matches.stream().map(match -> match.term).toList();
  }

  /**
   * Find all glossary matches in the provided tokens.
   *
   * @param tokens
   * @return list of {@link Match} objects representing the matches found
   */
  private List<Match> findGlossaryMatches(List<String> tokens, String originalText) {
    List<Match> matches = new ArrayList<>();
    for (int length = 1; length <= glossaryCache.getMaxNGramSize(); length++) {
      for (int start = 0; start <= tokens.size() - length; start++) {
        String nGram = String.join(" ", tokens.subList(start, start + length));
        if (glossaryCache.containsKey(nGram)) {
          List<GlossaryTerm> hits = glossaryCache.get(nGram);
          if (!hits.isEmpty()) {
            if (hits.size() > 1) {
              matches.add(handleMatchCollision(hits, originalText, start, length));
            } else {
              matches.add(new Match(start, start + length, hits.getFirst()));
            }
          }
        }
      }
    }

    return matches;
  }

  /**
   * Handle match collisions by checking if the nGram matches any of the matched terms directly (as
   * they've already been stem matched).
   *
   * <p>If no direct match is found, return the first stemmed match.
   */
  private Match handleMatchCollision(List<GlossaryTerm> hits, String text, int start, int length) {
    for (GlossaryTerm hit : hits) {
      if (text.toLowerCase().contains(hit.getText().toLowerCase())) {
        return new Match(start, start + length, hit);
      }
    }
    return new Match(start, start + length, hits.getFirst());
  }

  /**
   * Resolve overlapping matches by prioritizing longer, more specific phrases that fully overlap
   * other matching terms.
   *
   * @param matches
   * @return list of {@link Match} objects with overlaps resolved
   */
  private List<Match> resolveOverlaps(List<Match> matches) {
    matches.sort(Comparator.comparingInt((Match m) -> m.end - m.start).reversed());
    Set<Integer> coveredIndices = new HashSet<>();
    List<Match> finalMatches = new ArrayList<>();

    for (Match match : matches) {
      boolean fullyOverlaps = true;

      for (int i = match.start; i < match.end; i++) {
        if (!coveredIndices.contains(i)) {
          fullyOverlaps = false;
          break;
        }
      }

      if (!fullyOverlaps) {
        // This match is not fully covered by a previous larger match so it can be added to the
        // final matches
        finalMatches.add(match);
        for (int i = match.start; i < match.end; i++) {
          coveredIndices.add(i);
        }
      }
    }

    return finalMatches;
  }

  public void buildGlossaryCache() {
    glossaryCacheBuilder.buildCache();
    loadGlossaryCache();
  }

  public void loadGlossaryCache() {
    glossaryCache = glossaryCacheBlobStorage.getGlossaryCache().orElseGet(GlossaryCache::new);
  }

  /**
   * Data structure to hold the start and end indices of a match, along with the matched term.
   *
   * <p>This is used to resolve overlapping matches in the text.
   */
  private static class Match {
    final int start;
    final int end;
    final GlossaryTerm term;

    Match(int start, int end, GlossaryTerm term) {
      this.start = start;
      this.end = end;
      this.term = term;
    }
  }
}
