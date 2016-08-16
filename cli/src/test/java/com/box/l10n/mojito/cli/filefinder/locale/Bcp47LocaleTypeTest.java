package com.box.l10n.mojito.cli.filefinder.locale;

import java.util.regex.Pattern;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jaurambault
 */
public class Bcp47LocaleTypeTest {

    @Test
    public void testGetTargetLocaleRegex() {
        Bcp47LocaleType bcp47LocaleType = new Bcp47LocaleType();
        assertTrue(Pattern.matches(bcp47LocaleType.getTargetLocaleRegex(), "fr"));
        assertTrue(Pattern.matches(bcp47LocaleType.getTargetLocaleRegex(), "fr-FR"));
        assertTrue(Pattern.matches(bcp47LocaleType.getTargetLocaleRegex(), "zh-hans-CN"));
    }

}
