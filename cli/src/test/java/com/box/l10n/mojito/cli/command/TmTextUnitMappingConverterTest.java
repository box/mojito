package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.beust.jcommander.ParameterException;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TmTextUnitMappingConverterTest {

  @Test
  public void testConvertNull() {
    TmTextUnitMappingConverter tmTextUnitMappingConverter = new TmTextUnitMappingConverter();
    Map<Long, Long> convert = tmTextUnitMappingConverter.convert(null);
    assertNull(convert);
  }

  @Test
  public void testConvertValid() {
    TmTextUnitMappingConverter tmTextUnitMappingConverter = new TmTextUnitMappingConverter();
    Map<Long, Long> convert = tmTextUnitMappingConverter.convert("1001:2001;1002:2002");
    assertEquals(2001L, convert.get(1001L).longValue());
    assertEquals(2002L, convert.get(1002L).longValue());
    assertEquals(2, convert.size());
  }

  @Test
  public void multimapUnsupported() {
    TmTextUnitMappingConverter tmTextUnitMappingConverter = new TmTextUnitMappingConverter();
    Assertions.assertThatThrownBy(() -> tmTextUnitMappingConverter.convert("1001:2001;1001:2002"))
        .isInstanceOf(ParameterException.class)
        .hasMessage("Invalid source to target textunit id mapping [1001:2001;1001:2002]");
  }

  @Test(expected = ParameterException.class)
  public void testConvertInvalid() {
    TmTextUnitMappingConverter tmTextUnitMappingConverter = new TmTextUnitMappingConverter();
    Map<Long, Long> convert = tmTextUnitMappingConverter.convert("dsafdsf");
    assertEquals(0, convert.size());
  }
}
