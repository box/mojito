package com.box.l10n.mojito.okapi.filters;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jeanaurambault
 */
public class POFilterTest {

    @Test
    public void loadMsgIDPluralFromParent() {
        POFilter poFilter = new POFilter();
        poFilter.loadMsgIDPluralFromParent();
        assertNull(poFilter.msgIDPlural);
    }

    @Test
    public void loadMsgIDFromParent() {
        POFilter poFilter = new POFilter();
        poFilter.loadMsgIDFromParent();
        assertNull(poFilter.msgID);
    }

    @Test
    public void getUsagesFromSkeleton() {
        String skeleton = "#. Comments\n"
                + "#: core/logic/week_in_review_email_logic.py:49\n"
                + "#: core/logic/week_in_review_email_logic.py:72\n"
                + "msgid \"repin\"\n"
                + "msgid_plural \"repins\"\n"
                + "msgstr[0] \"repin-ru\"\n"
                + "msgstr[1] \"repins-ru-1\"\n"
                + "msgstr[2] \"repins-ru-2\"\n";

        POFilter poFilter = new POFilter();
        List<String> usages = new ArrayList<>(poFilter.getUsagesFromSkeleton(skeleton));
        
        assertEquals("core/logic/week_in_review_email_logic.py:49", usages.get(0));
        assertEquals("core/logic/week_in_review_email_logic.py:72", usages.get(1));
    }
    
    @Test
    public void getUsagesFromSkeletonNone() {
        String skeleton = "#. Comments\n"
                + "msgid \"repin\"\n"
                + "msgid_plural \"repins\"\n"
                + "msgstr[0] \"repin-ru\"\n"
                + "msgstr[1] \"repins-ru-1\"\n"
                + "msgstr[2] \"repins-ru-2\"\n";

        POFilter poFilter = new POFilter();
        List<String> usages = new ArrayList<>(poFilter.getUsagesFromSkeleton(skeleton));
        assertEquals(0, usages.size());
    }

}
