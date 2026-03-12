package com.box.l10n.mojito.okapi.filters.extraction;

import static org.assertj.core.groups.Tuple.tuple;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class PropertiesFilterExtractionTest extends FilterExtractionTestBase {

  @Test
  public void extractProperties() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> units =
        extract(
            "test.properties",
            "key1=value1\n" + "key2=value2",
            FilterConfigIdOverride.PROPERTIES_JAVA,
            null);

    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("key1", "value1"), tuple("key2", "value2"));
  }
}
