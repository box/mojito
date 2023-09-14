package com.box.l10n.mojito.okapi.extractor;

import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.Assert.*;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.asset.AssetPathToFilterConfigMapper;
import com.box.l10n.mojito.okapi.asset.FilterConfigurationMappers;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
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
    assertNotNull(assetExtractor);

    List<AssetExtractorTextUnit> assetExtractorTextUnitsForAsset =
        assetExtractor.getAssetExtractorTextUnitsForAsset(
            "test.properties",
            "key1=value1\n" + "key2=value2",
            FilterConfigIdOverride.PROPERTIES_JAVA,
            null,
            null);

    Assertions.assertThat(assetExtractorTextUnitsForAsset)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("key1", "value1"), tuple("key2", "value2"));
  }
}
