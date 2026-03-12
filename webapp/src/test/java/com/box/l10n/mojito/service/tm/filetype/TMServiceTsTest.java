package com.box.l10n.mojito.service.tm.filetype;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TMServiceTsTest extends TMServiceFileTypeTestBase {

  static Logger logger = LoggerFactory.getLogger(TMServiceTsTest.class);

  @Test
  public void testLocalizeTSFile() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        """
        namespace Translations {
            export const en = {
                // login comment
                "loginText": "Log In",
                // signup comment
                "signupText": "Sign up with `backquote`",
                "quotedText": "Hello \\"%s\\"",
                "noComment": "String with no comment\\nand newline",
                // template literals
                "templateText1": `one line`,
                "templateText2": `one line no comment`,
                "templateText3": `one line \\`/special\\` character`,
                // template multiline literals
                "templateMultilineText1": `first line
        second line`,
                // template multiline literals with escaped backquote
                "templateMultilineText2": `special character
        check \\`/command\\` out`,

            };
        }

        export default Translations;""";

    createAsset(repo, "translations.ts", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug(
          "{}\n{}=[{}]", textUnitDTO.getComment(), textUnitDTO.getName(), textUnitDTO.getSource());
    }

    assertEquals(9, textUnitDTOs.size());
    assertEquals("Sign up with `backquote`", textUnitDTOs.get(1).getSource());
    assertEquals("Hello \"%s\"", textUnitDTOs.get(2).getSource());
    assertEquals("String with no comment\nand newline", textUnitDTOs.get(3).getSource());
    assertEquals("special character\ncheck `/command` out", textUnitDTOs.get(8).getSource());

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(assetContent, localizedAsset);
  }
}
