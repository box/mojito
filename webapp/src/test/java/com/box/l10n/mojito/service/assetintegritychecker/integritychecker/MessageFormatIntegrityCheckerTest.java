package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;

public class MessageFormatIntegrityCheckerTest {

    @Test
    public void testCompilationCheckWorks() throws IntegrityCheckException {

        MessageFormatIntegrityChecker checker = new MessageFormatIntegrityChecker();
        String source = "{numFiles, plural, one{# There is one file} other{There are # files}}";
        String target = "{numFiles, plural, one{Il y a un fichier} other{Il y a # fichiers}}";

        checker.check(source, target);
    }

    @Test
    public void testCompilationCheckWorksWithMoreForms() throws IntegrityCheckException {

        MessageFormatIntegrityChecker checker = new MessageFormatIntegrityChecker();
        String source = "{numFiles, plural, one{# There is one file} other{There are # files}}";
        String target = "{numFiles, plural, zero{Il n'y a pas de fichier} one{Il y a un fichier} other{Il y a # fichiers}}";

        checker.check(source, target);
    }

    @Test
    public void testCompilationCheckFailsIfMissingBracket() throws IntegrityCheckException {

        MessageFormatIntegrityChecker checker = new MessageFormatIntegrityChecker();
        String source = "{numFiles, plural, one{# There is one file} other{There are # files}}";
        String target = "{numFiles, plural, one{Il y a un fichier} other{Il y a # fichiers}";

        try {
            checker.check(source, target);
            fail("MessageFormatIntegrityCheckerException must be thrown");
        } catch (MessageFormatIntegrityCheckerException e) {
            assertEquals(e.getMessage(), "Invalid pattern");
        }
    }

    @Test
    public void testCompilationCheckFailsIfPluralElementGetsTranslated() throws IntegrityCheckException {

        MessageFormatIntegrityChecker checker = new MessageFormatIntegrityChecker();
        String source = "{numFiles, plural, one{# There is one file} other{There are # files}}";
        String target = "{numFiles, plural, un{Il y a un fichier} autre{Il y a # fichiers}}";

        try {
            checker.check(source, target);
            fail("Exception must be thrown");
        } catch (MessageFormatIntegrityCheckerException e) {
            assertEquals(e.getMessage(), "Invalid pattern");
        }
    }
    
    @Test
    public void testNumberOfPlaceholder() throws MessageFormatIntegrityCheckerException {
        
        MessageFormatIntegrityChecker checker = new MessageFormatIntegrityChecker();
        String source = "At {1,time} on {1,date}, there was {2} on planet {0,number,integer}.";
        String target = "At {1,time} on {1,date}, there was {2} on planet {0,number,integer}.";
        
        checker.check(source, target);
    }

    @Test
    public void testWrongNumberOfPlaceholder() throws MessageFormatIntegrityCheckerException {
        
        MessageFormatIntegrityChecker checker = new MessageFormatIntegrityChecker();
        String source = "At {1,time} on {1,date}, there was {2} on planet {0,number,integer}.";
        String target = "At on {1,date}, there was {2} on planet {0,number,integer}.";
                
        try {
            checker.check(source, target);
            fail("Exception must be thrown");
        } catch (MessageFormatIntegrityCheckerException e) {
            assertEquals(e.getMessage(), "Number of placeholders in source (4) and target (3) is different");
        }
    }
  
}
