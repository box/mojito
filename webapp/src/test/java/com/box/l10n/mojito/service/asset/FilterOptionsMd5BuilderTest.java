package com.box.l10n.mojito.service.asset;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public class FilterOptionsMd5BuilderTest {

  FilterOptionsMd5Builder filterOptionsMd5Builder = new FilterOptionsMd5Builder();

  @Test
  public void md5Null() {
    assertEquals("d41d8cd98f00b204e9800998ecf8427e", filterOptionsMd5Builder.md5(null));
  }

  @Test
  public void md5() {
    assertEquals(
        "35c944668e75f79a7fbecc009cad04cb",
        filterOptionsMd5Builder.md5(Arrays.asList("opt1", "opt2")));
  }
}
