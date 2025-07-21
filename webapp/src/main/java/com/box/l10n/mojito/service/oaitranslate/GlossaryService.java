package com.box.l10n.mojito.service.oaitranslate;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GlossaryService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(GlossaryService.class);

  TextUnitSearcher textUnitSearcher;

  public GlossaryService(TextUnitSearcher textUnitSearcher) {
    this.textUnitSearcher = textUnitSearcher;
  }

  /**
   * Only include target and targetComment if included in localized file, ie not rejected. There
   * won't be a way to reject the source term in the UI, it has to be done with a repository push.
   *
   * @param glossaryRepositoryName
   * @param bcp47Locale The locale to load glossary terms for (e.g., "en-US")
   * @return A GlossaryTrie with all valid glossary terms for this locale
   */
  public GlossaryTrie loadGlossaryTrieForLocale(String glossaryRepositoryName, String bcp47Locale) {
    GlossaryTrie glossaryTrie = new GlossaryTrie();

    List<TextUnitDTO> textUnitDTOForGlossary =
        getTextUnitDTOForGlossary(glossaryRepositoryName, bcp47Locale);

    for (TextUnitDTO textUnitDTO : textUnitDTOForGlossary) {
      glossaryTrie.addTerm(
          new GlossaryTerm(
              textUnitDTO.getTmTextUnitId(),
              textUnitDTO.getName(),
              textUnitDTO.getSource(),
              textUnitDTO.getComment(),
              textUnitDTO.isIncludedInLocalizedFile() ? textUnitDTO.getTarget() : null,
              textUnitDTO.isIncludedInLocalizedFile() ? textUnitDTO.getTargetComment() : null));
    }

    return glossaryTrie;
  }

  /**
   * For glossaries, we get all terms, even if they don't have translation. Some terms can be
   * defined generally and apply to all locale, and some term may need specific translation per
   * locale.
   *
   * <p>A global DNT for example just need an entry for English.
   */
  List<TextUnitDTO> getTextUnitDTOForGlossary(String repositoryName, String bcp47Locale) {
    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();

    textUnitSearcherParameters.setRepositoryNames(List.of(repositoryName));
    textUnitSearcherParameters.setLocaleTags(List.of(bcp47Locale));
    textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);

    return textUnitSearcher.search(textUnitSearcherParameters);
  }

  public record GlossaryTerm(
      long tmTextUnitId,
      String name,
      String source,
      String comment,
      String target,
      String targetComment)
      implements CharTrie.Term {

    public boolean isDoNotTranslate() {
      return comment != null && comment.contains("DNT");
    }

    public String getPartOfSpeech() {
      if (comment == null || !comment.contains("\nPOS:")) {
        return null;
      }
      return comment.split("\nPOS:")[1];
    }

    @Override
    public String text() {
      return source;
    }
  }

  public static class GlossaryTrie extends CharTrie<GlossaryTerm> {}
}
