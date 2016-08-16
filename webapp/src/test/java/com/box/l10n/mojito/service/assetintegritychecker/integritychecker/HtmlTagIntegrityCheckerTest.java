package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jyi
 */
public class HtmlTagIntegrityCheckerTest {
    
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(HtmlTagIntegrityCheckerTest.class);
    
    HtmlTagIntegrityChecker checker = new HtmlTagIntegrityChecker();
    
    @Test
    public void testHtmlTagCheckWorks() {
        String source = "There are <b>%1</b> <u>files</u> and <b>%2</b> <i>folders</i>";
        String target = "Il y a <b>%1</b> <u>fichiers</u> et <b>%2</b> <i>dossiers</i>";

        checker.check(source, target);
    }
    
    @Test
    public void testHtmlTagCheckWorksWithDifferentOrders() {
        String source = "There are <b>%1</b> and <u>%2</u>";
        String target = "Il y a <u>%2</u> et <b>%1</b>";

        checker.check(source, target);
    }
    
    @Test(expected = HtmlTagIntegrityCheckerException.class)
    public void testHtmlTagCheckWorksWhenMissingAClosingTag() {
        String source = "There are <b>%1</b> files and %2 folders";
        String target = "Il y a <b>%1<b> fichiers et %2 dossiers";

        checker.check(source, target);
    }
    
    @Test(expected = HtmlTagIntegrityCheckerException.class)
    public void testHtmlTagCheckWorksWhenMissingATag() {
        String source = "There are <b>%1</b> files and %2 folders";
        String target = "Il y a %1 fichiers et %2 dossiers";

        checker.check(source, target);
    }
    
    @Test(expected = HtmlTagIntegrityCheckerException.class)
    public void testHtmlTagCheckWorksWhenTagIsModified() {
        String source = "There are <b>%1</b> files and %2 folders";
        String target = "Il y a <u>%1</u> fichiers et %2 dossiers";

        checker.check(source, target);
    }
}
