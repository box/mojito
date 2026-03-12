package com.box.l10n.mojito.service.tm.filetype;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TMServiceYamlTest extends TMServiceFileTypeTestBase {

  static Logger logger = LoggerFactory.getLogger(TMServiceYamlTest.class);

  @Test
  public void testLocalizeYAMLFile() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        "activerecord:\n"
            + "  errors:\n"
            + "    template:\n"
            + "      header:\n"
            + "        list: [one, two, three]\n"
            + "        map: {key: value, key2: value2}\n"
            + "        one: \"Impossible d'enregistrer {{model}}: 1 erreur\"\n"
            + "        other: \"Impossible d'enregistrer {{model}}: {{count}} erreurs.\"";

    createAsset(repo, "translations.yaml", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug(
          "{}\n{}=[{}]", textUnitDTO.getComment(), textUnitDTO.getName(), textUnitDTO.getSource());
    }

    assertEquals(7, textUnitDTOs.size());
    assertEquals("one", textUnitDTOs.get(0).getSource());
    assertEquals("two", textUnitDTOs.get(1).getSource());
    assertEquals("three", textUnitDTOs.get(2).getSource());
    assertEquals("value", textUnitDTOs.get(3).getSource());
    assertEquals("value2", textUnitDTOs.get(4).getSource());
    assertEquals("Impossible d'enregistrer {{model}}: 1 erreur", textUnitDTOs.get(5).getSource());
    assertEquals(
        "Impossible d'enregistrer {{model}}: {{count}} erreurs.", textUnitDTOs.get(6).getSource());

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(assetContent, localizedAsset);
  }

  @Test
  public void testLocalizeYAMLFileWithFilterOptions() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    List<String> filterOptions =
        Arrays.asList(
            "extractAllPairs=false", "exceptions=one|activerecord/errors/template/header/other");

    String assetContent =
        "activerecord:\n"
            + "  errors:\n"
            + "    template:\n"
            + "      header:\n"
            + "        list: [one, two, three]\n"
            + "        map: {key: value, key2: value2}\n"
            + "        one: \"Impossible d'enregistrer {{model}}: 1 erreur\"\n"
            + "        other: \"Impossible d'enregistrer {{model}}: {{count}} erreurs.\"";

    createAsset(repo, "translations.yaml", assetContent);
    processAsset(repo, assetContent, filterOptions);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.info(
          "{}\n{}=[{}]", textUnitDTO.getComment(), textUnitDTO.getName(), textUnitDTO.getSource());
    }

    assertEquals(2, textUnitDTOs.size());
    assertEquals("Impossible d'enregistrer {{model}}: 1 erreur", textUnitDTOs.get(0).getSource());
    assertEquals(
        "Impossible d'enregistrer {{model}}: {{count}} erreurs.", textUnitDTOs.get(1).getSource());

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(assetContent, localizedAsset);
  }
}
