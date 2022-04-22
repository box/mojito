package com.box.l10n.mojito.xml;

/**
 * Utility class to configure settings for XML and XPath operations.
 *
 * @author garion
 */
public class XmlParsingConfiguration {

    /**
     * Disables XML parsing limits introduced by JDK 8u331 in order to maintain backwards compatibility with
     * Okapi dependencies. See: https://www.oracle.com/java/technologies/javase/8u331-relnotes.html
     */
    public static void disableXPathLimits() {
        System.setProperty("jdk.xml.xpathExprGrpLimit", "0");
        System.setProperty("jdk.xml.xpathTotalOpLimit", "0");
        System.setProperty("jdk.xml.xpathExprOpLimit", "0");
    }
}
