package com.box.l10n.mojito.okapi.filters.extraction;

import com.box.l10n.mojito.okapi.ExtractUsagesFromTextUnitComments;
import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.asset.AssetPathToFilterConfigMapper;
import com.box.l10n.mojito.okapi.asset.FilterConfigurationMappers;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.okapi.extractor.AssetExtractor;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.okapi.filters.UnescapeUtils;
import java.util.List;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Base test class for Okapi filter extraction tests.
 *
 * <p>Test setup is based on the {@link com.box.l10n.mojito.okapi.extractor.AssetExtractorTest}.
 *
 * <p>Sets up the Spring context with {@link AssetExtractor} and all filter dependencies, and
 * provides a helper method for running extraction against supplied file content.
 *
 * <p>DirtiesContext is required to avoid the Okapi filter tests being run as SpringBootTests and
 * failing due to dependency errors if this test runs before them.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {
      AssetExtractor.class,
      AssetPathToFilterConfigMapper.class,
      FilterConfigurationMappers.class,
      TextUnitUtils.class,
      UnescapeUtils.class,
      ExtractUsagesFromTextUnitComments.class
    })
@EnableSpringConfigured
@DirtiesContext
public abstract class FilterExtractionTestBase {

  @Autowired private AssetExtractor assetExtractor;

  protected List<AssetExtractorTextUnit> extract(
      String assetPath,
      String content,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions)
      throws UnsupportedAssetFilterTypeException {
    return assetExtractor.getAssetExtractorTextUnitsForAsset(
        assetPath, content, filterConfigIdOverride, filterOptions);
  }
}
