package com.box.l10n.mojito.okapi.extractor;

import static org.assertj.core.groups.Tuple.tuple;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.asset.AssetPathToFilterConfigMapper;
import com.box.l10n.mojito.okapi.asset.FilterConfigurationMappers;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {
      AssetExtractor.class,
      AssetPathToFilterConfigMapper.class,
      FilterConfigurationMappers.class,
      TextUnitUtils.class,
      AssetExtractorTest.class
    })
@EnableSpringConfigured
public class AssetExtractorTest {

  @Autowired AssetExtractor assetExtractor;

  @Test
  public void extractProperties() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> assetExtractorTextUnitsForAsset =
        assetExtractor.getAssetExtractorTextUnitsForAsset(
            "test.properties",
            "key1=value1\n" + "key2=value2",
            FilterConfigIdOverride.PROPERTIES_JAVA,
            null);

    Assertions.assertThat(assetExtractorTextUnitsForAsset)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("key1", "value1"), tuple("key2", "value2"));
  }

  @Test
  public void extractPropertiesWithCoded() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> assetExtractorTextUnitsForAsset =
        assetExtractor.getAssetExtractorTextUnitsForAsset(
            "test.json",
            "{\"hello\" : \"Hello %s!\" }\n",
            null,
            Arrays.asList(
                "convertToHtmlCodes=true",
                "codeFinderData=#v1\n"
                    + "count.i=3\n"
                    + "rule0=%(([-0+#]?)[-0+#]?)((\\d\\$)?)(([\\d\\*]*)(\\.[\\d\\*]*)?)[dioxXucsfeEgGpn]\n"
                    + "rule1=(\\\\r\\\\n)|\\\\a|\\\\b|\\\\f|\\\\n|\\\\r|\\\\t|\\\\v\n"
                    + "rule2=\\{\\d.*?\\}\n"
                    + "sample=%s, %d, {1}, \\n, \\r, \\t, etc.\n"
                    + "useAllRulesWhenTesting.b=false"));

    Assertions.assertThat(assetExtractorTextUnitsForAsset)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("hello", "Hello <br id='p1'/>!"));
  }

  @Test
  public void extractHtmlAlpha() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> assetExtractorTextUnitsForAsset =
        assetExtractor.getAssetExtractorTextUnitsForAsset(
            "test.html",
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
                + "<table><tr><td style=\"font-size:0px\" class=\"nomob\">&nbsp;</td></tr></table>\n"
                + "<table><tr><td></td></tr></table>\n"
                + "<table><tr><td></td>  </tr></table>\n"
                + "</body>\n"
                + "</html>",
            FilterConfigIdOverride.HTML_ALPHA,
            null);

    Assertions.assertThat(assetExtractorTextUnitsForAsset)
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
  public void genIdForTest() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> assetExtractorTextUnitsForAsset =
        assetExtractor.getAssetExtractorTextUnitsForAsset(
            "test.html",
            "<html>\n"
                + "  <p>100 character description:</p>\n"
                + "  <ul>\n"
                + "    <li>15 min</li>\n"
                + "    <li>1 day</li>\n"
                + "    <li>1 hour</li>\n"
                + "    <li>1 month</li>\n"
                + "  </ul>\n"
                + "  <p>\n"
                + "    Image in text <img src=\"image.jpg\" alt=\"Alt image\">.\n"
                + "  </p>\n"
                + "</html>",
            FilterConfigIdOverride.HTML_ALPHA,
            Arrays.asList("processImageUrls=true"));

    Assertions.assertThat(assetExtractorTextUnitsForAsset)
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
  public void documentPartExtraction() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> assetExtractorTextUnitsForAsset =
        assetExtractor.getAssetExtractorTextUnitsForAsset(
            "test.html",
            "<html>\n"
                + "  <p>\n"
                + "    Image in text <img src=\"image.jpg\" alt=\"Alt image\">.\n"
                + "  </p>\n"
                + "</html>",
            FilterConfigIdOverride.HTML_ALPHA,
            Arrays.asList("processImageUrls=true"));

    Assertions.assertThat(assetExtractorTextUnitsForAsset)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(
            tuple(
                "8f1bdae06589d55b62184a76e0e70d0e-d41d8cd98f00b204e9800998ecf8427e-1", "Alt image"),
            tuple(
                "0d5b1c4c7f720f698946c7f6ab08f687-8f1bdae06589d55b62184a76e0e70d0e-1", "image.jpg"),
            tuple(
                "34a6a48789dd1ff7dff813a8fb627b91-0d5b1c4c7f720f698946c7f6ab08f687-1",
                "Image in text <br id='p1'/>."));
  }

  @Test
  public void documentNoPartExtraction() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> assetExtractorTextUnitsForAsset =
        assetExtractor.getAssetExtractorTextUnitsForAsset(
            "test.html",
            "<html>\n"
                + "  <p>\n"
                + "    Image in text <img src=\"image.jpg\" alt=\"Alt image\">.\n"
                + "  </p>\n"
                + "</html>",
            FilterConfigIdOverride.HTML_ALPHA,
            null);

    Assertions.assertThat(assetExtractorTextUnitsForAsset)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(
            tuple(
                "8f1bdae06589d55b62184a76e0e70d0e-d41d8cd98f00b204e9800998ecf8427e-1", "Alt image"),
            tuple(
                "34a6a48789dd1ff7dff813a8fb627b91-8f1bdae06589d55b62184a76e0e70d0e-1",
                "Image in text <br id='p1'/>."));
  }
}
