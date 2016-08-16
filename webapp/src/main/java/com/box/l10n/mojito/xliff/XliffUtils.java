package com.box.l10n.mojito.xliff;

import java.io.StringReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Utilities for working with XLIFF.
 *
 * @author jaurambault
 */
@Component
public class XliffUtils {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(XliffUtils.class);

    /**
     * Gets the target language of the XLIFF by looking at the first "file" 
     * element.
     *
     * @param xliffContent xliff content from which to extract the target language
     * @return the target language or {@code null} if not found
     */
    public String getTargetLanguage(String xliffContent) {

        String targetLanguage = null;

        InputSource inputSource = new InputSource(new StringReader(xliffContent));
        XPath xPath = XPathFactory.newInstance().newXPath();
        
        SimpleNamespaceContext simpleNamespaceContext = new SimpleNamespaceContext();
        simpleNamespaceContext.bindNamespaceUri("xlf", "urn:oasis:names:tc:xliff:document:1.2");
        xPath.setNamespaceContext(simpleNamespaceContext);
        
        try {
            Node node = (Node) xPath.evaluate("/xlf:xliff/xlf:file[1]/@target-language", inputSource, XPathConstants.NODE);
            
            if(node != null) {
                targetLanguage = node.getTextContent();
            }
            
        } catch (XPathExpressionException xpee) {
            logger.debug("Can't extract target language from xliff", xpee);
        }
        return targetLanguage;
    }
}
