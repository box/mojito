package com.box.l10n.mojito.service.tm.localizeasset;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalizeHtmlAssetTest extends LocalizeAssetTestBase {

  static Logger logger = LoggerFactory.getLogger(LocalizeHtmlAssetTest.class);

  @Test
  public void testLocalizeHtmlFilter() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        """
        <!DOCTYPE html>
        <html>
        <head>
            <title>My Title</title>
            <meta name="description" content="My description"/>
            <meta name="author" content="My author"/>
            <meta name="keywords" content="My keywords"/>
            <link rel="stylesheet" href="./stylesheet.css" type="text/css"/>
            <style>.body {
                width: auto;
            }</style>
        </head>
        <body>
        <p>thi is the first paragraph</p>
        <p>this is the second paragraph. With an <img src="someimage.jpg"> inside text</p>
        <ul>
            <li>item1</li>
            <li>item2</li>
        </ul>
        </body>
        </html>""";

    createAsset(repo, "demo.html", assetContent);
    processAsset(repo, assetContent, FilterConfigIdOverride.HTML_ALPHA, null);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.info(
          "{}\n{}=[{}]", textUnitDTO.getComment(), textUnitDTO.getName(), textUnitDTO.getSource());
    }

    String localizedAsset =
        generateLocalized(
            assetContent, repoLocale, "en-GB", FilterConfigIdOverride.HTML_ALPHA, null);
    logger.info("localized=\n{}", localizedAsset);

    // Okapi adds meta tag in that case? this could be a problem or not, just putting a note here
    // for now. Did not
    // see that happen in CLI tests
    localizedAsset =
        localizedAsset.replace(
            "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">", "");
    assertEquals(assetContent, localizedAsset);
  }
}
