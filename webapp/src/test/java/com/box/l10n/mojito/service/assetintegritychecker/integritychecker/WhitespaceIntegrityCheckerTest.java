package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jyi
 */
public class WhitespaceIntegrityCheckerTest {
    
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(WhitespaceIntegrityCheckerTest.class);

    WhitespaceIntegrityChecker checker = new WhitespaceIntegrityChecker();

    @Test
    public void testWhitespaceIntegrityCheckerWorksWithNoLeadingTrailingWhitespaces() {
        String source = "There are %1 files and %2 folders";
        String target = "Il y a %1 fichiers et %2 dossiers";
        checker.check(source, target);
    }
    
    @Test
    public void testWhitespaceIntegrityCheckerWorksWithLeadingTrailingWhitespaces() {
        String source = " There are %1 files and %2 folders\n";
        String target = " Il y a %1 fichiers et %2 dossiers\n";
        checker.check(source, target);
    }

    @Test
    public void testWhitespaceIntegrityCheckerWorksWithMultipleLeadingTrailingWhitespaces() {
        String source = "\t  There are %1 files and %2 folders  \n";
        String target = "\t  Il y a %1 fichiers et %2 dossiers  \n";
        checker.check(source, target);
    }
    
    @Test(expected = WhitespaceIntegrityCheckerException.class)
    public void testWhitespaceIntegrityCheckerWorksWithDifferentLeadingTrailingWhitespaces1() {
        String source = " There are %1 files and %2 folders \n";
        String target = "  Il y a %1 fichiers et %2 dossiers\n";
        checker.check(source, target);
    }
    
    @Test(expected = WhitespaceIntegrityCheckerException.class)
    public void testWhitespaceIntegrityCheckerWorksWithDifferentLeadingTrailingWhitespaces2() {
        String source = "\nThere are %1 files and %2 folders ";
        String target = " Il y a %1 fichiers et %2 dossiers\n";
        checker.check(source, target);
    }
    
    @Test
    public void testLeadingWhitespaceCheckWorks() {
        String source = "\n There are %1 files and %2 folders";
        String target = "\n Il y a %1 fichiers et %2 dossiers";

        checker.check(source, target);
    }

    @Test
    public void testLeadingWhitespaceCheckWorksWithoutLeadingWhitespaces() {
        String source = "There are %1 files and %2 folders";
        String target = "Il y a %1 fichiers et %2 dossiers";

        checker.check(source, target);
    }

    @Test(expected = WhitespaceIntegrityCheckerException.class)
    public void testLeadingWhitespaceCheckCatchesRemovedNewline() {
        String source = "\nThere are %1 files and %2 folders";
        String target = "Il y a %1 fichiers et %2 dossiers";

        checker.check(source, target);
    }

    @Test(expected = WhitespaceIntegrityCheckerException.class)
    public void testLeadingWhitespaceCheckCatchesRemovedSpace() {
        String source = " There are %1 files and %2 folders";
        String target = "Il y a %1 fichiers et %2 dossiers";

        checker.check(source, target);
    }

    @Test(expected = WhitespaceIntegrityCheckerException.class)
    public void testLeadingWhitespaceCheckCatchesDifferentLeadingWhitespaces() {
        String source = " \nThere are %1 files and %2 folders";
        String target = "\n Il y a %1 fichiers et %2 dossiers";

        checker.check(source, target);
    }
    
    @Test
    public void testTrailingWhitespaceCheckWorks() {
        String source = "There are %1 files and %2 folders \n";
        String target = "Il y a %1 fichiers et %2 dossiers \n";

        checker.check(source, target);
    }

    @Test
    public void testTrailingWhitespaceCheckWorksWithoutTrailingWhitespaces() {
        String source = "There are %1 files and %2 folders";
        String target = "Il y a %1 fichiers et %2 dossiers";

        checker.check(source, target);
    }

    @Test(expected = WhitespaceIntegrityCheckerException.class)
    public void testTrailingWhitespaceCheckCatchesRemovedNewline() {
        String source = "There are %1 files and %2 folders\n";
        String target = "Il y a %1 fichiers et %2 dossiers";

        checker.check(source, target);
    }

    @Test(expected = WhitespaceIntegrityCheckerException.class)
    public void testTrailingWhitespaceCheckCatchesRemovedSpace() {
        String source = "There are %1 files and %2 folders ";
        String target = "Il y a %1 fichiers et %2 dossiers";

        checker.check(source, target);
    }

    @Test(expected = WhitespaceIntegrityCheckerException.class)
    public void testTrailingWhitespaceCheckCatchesDifferentTrailingWhitespaces() {
        String source = "There are %1 files and %2 folders \n";
        String target = "Il y a %1 fichiers et %2 dossiers\n ";

        checker.check(source, target);
    }
    
}
