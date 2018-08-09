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

    @Test
    public void testHtmlTagCheckWorksForOtherHtml() {
        String source = "There are <span class=\"foo\">%1</span> files and %2 folders";
        String target = "Il y a <span class=\"foo\">%1</span> fichiers et %2 dossiers";

        checker.check(source, target);
    }

    @Test
    public void testHtmlTagCheckWorksForRandomXml() {
        String source = "There are <abc attribute=\"foo\">%1</abc> files and %2 folders";
        String target = "Il y a <abc attribute=\"foo\">%1</abc> fichiers et %2 dossiers";

        checker.check(source, target);
    }

    @Test
    public void testHtmlTagCheckWorksForSelfClosingTags() {
        String source = "There are <p/>%1 files and %2 folders";
        String target = "Il y a <p/>%1 fichiers et %2 dossiers";

        checker.check(source, target);
    }

    @Test
    public void testHtmlTagCheckWorksForUnmatchedTags() {
        String source = "There are <img src=\"image.jpg\">%1 files and %2 folders";
        String target = "Il y a <img src=\"image.jpg\">%1 fichiers et %2 dossiers";

        checker.check(source, target);
    }

    @Test
    public void testHtmlTagCheckWorksForNestedTags() {
        String source = "<div id=\"asdf\">There are <span class=\"foo\">%1</span> files and %2 folders</div>";
        String target = "<div id=\"asdf\">Il y a <span class=\"foo\">%1</span> fichiers et %2 dossiers</div>";

        checker.check(source, target);
    }

    @Test(expected = HtmlTagIntegrityCheckerException.class)
    public void testHtmlTagCheckMissingAttributes() {
        String source = "<div id=\"asdf\">There are <span class=\"foo\">%1</span> files and %2 folders</div>";
        String target = "<div>Il y a <span class=\"foo\">%1</span> fichiers et %2 dossiers</div>";

        checker.check(source, target);
    }

    @Test
    public void testHtmlTagCheckWorksForNoTags() throws Exception {
        String source = "There are %1 files and %2 folders";
        String target = "Il y a %1 fichiers et %2 dossiers";

        checker.check(source, target);
    }

    @Test
    public void testHtmlTagCheckWorksForNoValueAttributes() {
        String source = "<option selected>There are <span class=\"foo\">%1</span> files and %2 folders</option>";
        String target = "<option selected>Il y a <span class=\"foo\">%1</span> fichiers et %2 dossiers</option>";

        checker.check(source, target);
    }

    @Test(expected = HtmlTagIntegrityCheckerException.class)
    public void testHtmlTagCheckIncorrectTagNameCase() {
        String source = "<span class=\"asdf\">There are <span class=\"foo\">%1</span> files and %2 folders</option>";
        String target = "<Span class=\"asdf\">Il y a <span class=\"foo\">%1</span> fichiers et %2 dossiers</option>";

        checker.check(source, target);
    }

    @Test
    public void testHtmlTagCheckWhiteSpaceInAttributes() {
        String source = "<div id = \"asdf\">There are <span class = \"foo\">%1</span> files and %2 folders</div>";
        String target = "<div id = \"asdf\">Il y a <span class = \"foo\">%1</span> fichiers et %2 dossiers</div>";

        checker.check(source, target);
    }

    @Test(expected = HtmlTagIntegrityCheckerException.class)
    public void testHtmlTagCheckWhiteSpaceIsSame() {
        String source = "<div id=\"asdf\">There are <span class=\"foo\">%1</span> files and %2 folders</div>";
        String target = "<div id = \"asdf\">Il y a <span class = \"foo\">%1</span> fichiers et %2 dossiers</div>";

        checker.check(source, target);
    }

    @Test(expected = HtmlTagIntegrityCheckerException.class)
    public void testHtmlTagCheckCloseTagsSame() {
        String source = "<div id=\"asdf\">There are <span class=\"foo\">%1</span> files and %2 folders</div>";
        String target = "<div id=\"asdf\">Il y a <span class=\"foo\">%1</Span> fichiers et %2 dossiers</Div>";

        checker.check(source, target);
    }

    @Test
    public void testHtmlTagCheckNonTagLessThanDoesntConfuseThings() {
        String source = "Upload is <10% complete.";
        String target = "Le téléchargement est terminé < 10 %.";

        checker.check(source, target);
    }

}
