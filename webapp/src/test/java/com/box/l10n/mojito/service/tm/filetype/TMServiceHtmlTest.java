package com.box.l10n.mojito.service.tm.filetype;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TMServiceHtmlTest extends TMServiceFileTypeTestBase {

  static Logger logger = LoggerFactory.getLogger(TMServiceHtmlTest.class);

  @Test
  public void testLocalizeHtmlFilter() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        "<!DOCTYPE html>\n"
            + "<html>\n"
            + "<head>\n"
            + "    <title>My Title</title>\n"
            + "    <meta name=\"description\" content=\"My description\"/>\n"
            + "    <meta name=\"author\" content=\"My author\"/>\n"
            + "    <meta name=\"keywords\" content=\"My keywords\"/>\n"
            + "    <link rel=\"stylesheet\" href=\"./stylesheet.css\" type=\"text/css\"/>\n"
            + "    <style>.body {\n"
            + "        width: auto;\n"
            + "    }</style>\n"
            + "</head>\n"
            + "<body>\n"
            + "<p>thi is the first paragraph</p>\n"
            + "<p>this is the second paragraph. With an <img src=\"someimage.jpg\"> inside text</p>\n"
            + "<ul>\n"
            + "    <li>item1</li>\n"
            + "    <li>item2</li>\n"
            + "</ul>\n"
            + "</body>\n"
            + "</html>";

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
