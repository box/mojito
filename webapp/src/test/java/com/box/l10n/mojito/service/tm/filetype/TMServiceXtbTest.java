package com.box.l10n.mojito.service.tm.filetype;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TMServiceXtbTest extends TMServiceFileTypeTestBase {

  static Logger logger = LoggerFactory.getLogger(TMServiceXtbTest.class);

  @Test
  public void testLocalizeXtb() throws Exception {
    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE translationbundle>
        <translationbundle lang="en-US">
        	<translation id="0" key="MSG_DIALOG_OK_" source="lib/closure-library/closure/goog/ui/dialog.js" desc="Standard caption for the dialog 'OK' button.">OK</translation>
             <translation id="1" key="MSG_VIEWER_MENU" source="src/js/box/dicom/viewer/toolbar.js" desc="Tooltip text for the &quot;More&quot; menu.">More</translation>
             <translation id="2" key="MSG_GONSTEAD_STEP" source="src/js/box/dicom/viewer/gonsteaddialog.js" desc="Instructions for the Gonstead method.">Select the &lt;strong&gt;left Iliac crest&lt;/strong&gt;</translation>
        </translationbundle>""";
    String expectedContent =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <translationbundle lang="en-US">
        	<translation desc="Standard caption for the dialog 'OK' button." id="0" key="MSG_DIALOG_OK_" source="lib/closure-library/closure/goog/ui/dialog.js">OK</translation>
             <translation desc="Tooltip text for the &quot;More&quot; menu." id="1" key="MSG_VIEWER_MENU" source="src/js/box/dicom/viewer/toolbar.js">More</translation>
             <translation desc="Instructions for the Gonstead method." id="2" key="MSG_GONSTEAD_STEP" source="src/js/box/dicom/viewer/gonsteaddialog.js">Select the &lt;strong&gt;left Iliac crest&lt;/strong&gt;</translation>
        </translationbundle>""";
    createAsset(repo, "xtb/messages-en-US.xtb", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    assertEquals(3, textUnitDTOs.size());
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedContent, localizedAsset);
  }

  @Ignore("Bug: does not output translationbundle opening tag")
  @Test
  public void testLocalizeXtbRemoveUntranslated() throws Exception {
    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE translationbundle>
        <translationbundle lang="en-US">
        	<translation id="0" key="MSG_DIALOG_OK_" source="lib/closure-library/closure/goog/ui/dialog.js" desc="Standard caption for the dialog 'OK' button.">OK</translation>
             <translation id="1" key="MSG_VIEWER_MENU" source="src/js/box/dicom/viewer/toolbar.js" desc="Tooltip text for the &quot;More&quot; menu.">More</translation>
             <translation id="2" key="MSG_GONSTEAD_STEP" source="src/js/box/dicom/viewer/gonsteaddialog.js" desc="Instructions for the Gonstead method.">Select the &lt;strong&gt;left Iliac crest&lt;/strong&gt;</translation>
        </translationbundle>""";
    String expectedContent =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <translationbundle />
        """;

    createAsset(repo, "xtb/messages-en-US.xtb", assetContent);
    processAsset(repo, assetContent);

    String localizedAsset = generateLocalizedRemoveUntranslated(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    // assertEquals(expectedContent, localizedAsset);

    String forImport =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE translationbundle>
        <translationbundle lang="ja-JP">
        <translation id="1" key="MSG_VIEWER_MENU" source="src/js/box/dicom/viewer/toolbar.js" desc="Tooltip text for the &quot;More&quot; menu.">Plus</translation>
        </translationbundle>""";

    tmService
        .importLocalizedAssetAsync(
            assetId,
            forImport,
            repoLocale.getLocale().getId(),
            StatusForEqualTarget.TRANSLATION_NEEDED,
            null,
            null)
        .get();

    localizedAsset = generateLocalizedRemoveUntranslated(assetContent, repoLocale, "ja-JP");
    logger.debug("localized after import=\n{}", localizedAsset);
    assertEquals(forImport, localizedAsset);
  }
}
