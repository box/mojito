package com.box.l10n.mojito.okapi.filters.extraction;

import static org.assertj.core.groups.Tuple.tuple;

import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class JSONFilterExtractionTest extends FilterExtractionTestBase {

  @Test
  public void extractWithCodeFinder() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> units =
        extract(
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

    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("hello", "Hello <br id='p1'/>!"));
  }
}
