package com.box.l10n.mojito.xliff;

import static org.junit.Assert.*;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author jaurambault
 */
public class XliffUtilsTest {

  XliffUtils xliffUtils;

  final String xliff =
      "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
          + "<file original=\"en.properties\" source-language=\"en\" target-language=\"fr-fr\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
          + "<body>\n"
          + "<trans-unit id=\"\" resname=\"fake\">\n"
          + "<source xml:lang=\"en\">fake</source>\n"
          + "<target xml:lang=\"\">fake</target>\n"
          + "<note annotates=\"target\" from=\"automation\">MUST REVIEW\n"
          + "Text unit for id: , Skipping it...</note>\n"
          + "</trans-unit>\n"
          + "</body>\n"
          + "</file>\n"
          + "</xliff>";

  @Before
  public void setUp() {
    this.xliffUtils = new XliffUtils();
  }

  @Test
  public void testGetXliffTargetLanguage() throws XPathExpressionException {
    assertEquals("fr-fr", this.xliffUtils.getTargetLanguage(this.xliff));
  }

  @Test
  public void testGetXliffTargetLanguageMissingAttribute()
      throws XPathExpressionException,
          ParserConfigurationException,
          IOException,
          TransformerException,
          SAXException {
    String xliffWithoutTargetLanguage = this.xliffUtils.removeAttribute(xliff, "target-language");

    assertEquals(null, this.xliffUtils.getTargetLanguage(xliffWithoutTargetLanguage));
  }

  @Test
  public void testRemoveAttribute()
      throws XPathExpressionException,
          ParserConfigurationException,
          IOException,
          TransformerException,
          SAXException {
    String newXliff = this.xliffUtils.removeAttribute(this.xliff, "target-language");
    assertFalse(newXliff.contains("target-language=\"fr-fr\""));
  }

  @Test
  public void testRemoveAttributeWithInvalidAttributeName()
      throws XPathExpressionException,
          ParserConfigurationException,
          IOException,
          TransformerException,
          SAXException {
    String newXliff = this.xliffUtils.removeAttribute(this.xliff, "invalid-target-language");
    assertTrue(newXliff.contains("target-language=\"fr-fr\""));
  }
}
