package com.box.l10n.mojito.okapi.filters.extraction;

import static org.assertj.core.groups.Tuple.tuple;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class HtmlFilterExtractionTest extends FilterExtractionTestBase {

  @Test
  public void extractHtmlAlpha() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> units =
        extract(
            "test.html",
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
                <table><tr><td style="font-size:0px" class="nomob">&nbsp;</td></tr></table>
                <table><tr><td></td></tr></table>
                <table><tr><td></td>  </tr></table>
                </body>
                </html>""",
            FilterConfigIdOverride.HTML_ALPHA,
            null);

    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(
            tuple(
                "5badc643b79fdda9775c45b46540afc0-d41d8cd98f00b204e9800998ecf8427e-1", "My Title"),
            tuple(
                "5c23e253f3b4fe9534c63e11d30dc63d-5badc643b79fdda9775c45b46540afc0-1",
                "My description"),
            tuple(
                "16d3282564595ba5c7c6a10836184c53-5c23e253f3b4fe9534c63e11d30dc63d-1",
                "My keywords"),
            tuple(
                "2d8882d892e7be7918918e95daf588eb-16d3282564595ba5c7c6a10836184c53-1",
                "thi is the first paragraph"),
            tuple(
                "267d84d477728ad18c388c58b223957a-2d8882d892e7be7918918e95daf588eb-1",
                "this is the second paragraph. With an <br id='p1'/> inside text"),
            tuple("cabf67b0be34694cd96a9ec1c0ef766e-267d84d477728ad18c388c58b223957a-1", "item1"),
            tuple("235bacd9fe81ea549903a51e673bdbb9-cabf67b0be34694cd96a9ec1c0ef766e-1", "item2"));
  }

  @Test
  public void extractWithGeneratedIds() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> units =
        extract(
            "test.html",
            """
                <html>
                  <p>100 character description:</p>
                  <ul>
                    <li>15 min</li>
                    <li>1 day</li>
                    <li>1 hour</li>
                    <li>1 month</li>
                  </ul>
                  <p>
                    Image in text <img src="image.jpg" alt="Alt image">.
                  </p>
                </html>""",
            FilterConfigIdOverride.HTML_ALPHA,
            Arrays.asList("processImageUrls=true"));

    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(
            tuple(
                "c567033d6600a8627d595a45a8713ac9-d41d8cd98f00b204e9800998ecf8427e-1",
                "100 character description:"),
            tuple("17ddc0b964b763e4bab07f917de55e13-c567033d6600a8627d595a45a8713ac9-1", "15 min"),
            tuple("e3b481d5297f475abc283227bedbd9b9-17ddc0b964b763e4bab07f917de55e13-1", "1 day"),
            tuple("72ab9d0304d3e84c6aa2dd15eda282f2-e3b481d5297f475abc283227bedbd9b9-1", "1 hour"),
            tuple("1634e66b522edaa910370cc5581a47f1-72ab9d0304d3e84c6aa2dd15eda282f2-1", "1 month"),
            tuple(
                "8f1bdae06589d55b62184a76e0e70d0e-1634e66b522edaa910370cc5581a47f1-1", "Alt image"),
            tuple(
                "0d5b1c4c7f720f698946c7f6ab08f687-8f1bdae06589d55b62184a76e0e70d0e-1", "image.jpg"),
            tuple(
                "88a3a4caac9d7f100871689d2c18212a-0d5b1c4c7f720f698946c7f6ab08f687-1",
                "Image in text <br id='p1'/>."));
  }

  @Test
  public void checkDoNotTranslate() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> units =
        extract(
            "test.html",
            """
                <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN"
                    "http://www.w3.org/TR/REC-html40/loose.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <body>
                <table>
                  <tr>
                    <td>
                      <a title="Visit" href="https://www.somesite.com">
                      <img src="https://www.somesite.com/img.png">
                    </a>
                    </td>
                    <td>
                      <img src="https://www.somesite.com/img2.png">
                    </td>
                  </tr>
                </table>
                </body>
                </html>
                """,
            FilterConfigIdOverride.HTML_ALPHA,
            Arrays.asList("processImageUrls=false"));

    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(
            tuple("5e706a9b7eb9b142a619e4331d6976e6-d41d8cd98f00b204e9800998ecf8427e-1", "Visit"));

    // without filtering according to Okapi "hasText()", it would return translatable text unit
    // with the following content:
    // tuple("93e7c3bf7f959f5dbd981361605b4b58-5e706a9b7eb9b142a619e4331d6976e6-1",
    // "<u id='1'> <br id='p2'/> </u>"),
    // tuple("e063c00ad4343fedbb80d79301575724-93e7c3bf7f959f5dbd981361605b4b58-1",
    // "<br id='p1'/>"))
  }
}
