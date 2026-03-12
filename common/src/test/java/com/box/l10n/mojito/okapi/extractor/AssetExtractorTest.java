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
import org.springframework.test.annotation.DirtiesContext;
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
/**
 * DirtiesContext is required to avoid the Okapi filter tests being run as SpringBootTests and
 * failing due to dependency errors if this test runs before them.
 */
@DirtiesContext
public class AssetExtractorTest {

  @Autowired AssetExtractor assetExtractor;

  @Test
  public void documentPartExtraction() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> assetExtractorTextUnitsForAsset =
        assetExtractor.getAssetExtractorTextUnitsForAsset(
            "test.html",
            """
                <html>
                  <p>
                    Image in text <img src="image.jpg" alt="Alt image">.
                  </p>
                </html>""",
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
            """
                <html>
                  <p>
                    Image in text <img src="image.jpg" alt="Alt image">.
                  </p>
                </html>""",
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
