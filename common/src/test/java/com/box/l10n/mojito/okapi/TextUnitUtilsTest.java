package com.box.l10n.mojito.okapi;

import static org.junit.Assert.*;

import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TextUnitUtilsTest {

  @Test
  public void getSourceAsCodedHtml() {
    TextUnitUtils textUnitUtils = new TextUnitUtils();
    TextUnit textUnit = getTestTextUnit();
    String sourceAsCodedHtml = textUnitUtils.getSourceAsCodedHtml(textUnit);
    Assertions.assertThat(sourceAsCodedHtml).isEqualTo("Hi <br id='p1'/>!");
  }

  @Test
  public void fromCodedHTML() {
    TextUnitUtils textUnitUtils = new TextUnitUtils();
    TextUnit textUnit = getTestTextUnit();
    TextFragment translationTextFragment =
        textUnitUtils.fromCodedHTML(textUnit, "<br id='p1'/>, bonjour !");
    Assertions.assertThat(translationTextFragment.toText()).isEqualTo("{username}, bonjour !");
  }

  private static TextUnit getTestTextUnit() {
    TextFragment textFragment = new TextFragment();
    textFragment
        .append("Hi ")
        .append(new Code(TextFragment.TagType.PLACEHOLDER, "MF", "{username}"))
        .append("!");
    TextUnit textUnit = new TextUnit();
    textUnit.setSourceContent(textFragment);
    return textUnit;
  }
}
