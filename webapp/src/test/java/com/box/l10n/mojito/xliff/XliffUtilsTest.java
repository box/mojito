package com.box.l10n.mojito.xliff;

import javax.xml.xpath.XPathExpressionException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jaurambault
 */
public class XliffUtilsTest {

    @Test
    public void testGetXliffTargetLanguage() throws XPathExpressionException {
        
        XliffUtils xliffUtils = new XliffUtils();

        String xliff = "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
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

        assertEquals("fr-fr", xliffUtils.getTargetLanguage(xliff));
    }

    @Test
    public void testGetXliffTargetLanguageMissingAttribute() throws XPathExpressionException {
        
        XliffUtils xliffUtils = new XliffUtils();

        String xliff = "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                + "<file original=\"en.properties\" source-language=\"en\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
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

        assertEquals(null, xliffUtils.getTargetLanguage(xliff));
    }

}
