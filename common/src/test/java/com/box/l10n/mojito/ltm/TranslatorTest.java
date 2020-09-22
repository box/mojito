package com.box.l10n.mojito.ltm;

import org.junit.Test;

import java.util.ListResourceBundle;

import static com.box.l10n.mojito.ltm.GlobalTranslator.*;
import static com.box.l10n.mojito.ltm.MyKeys.STRING1;

public class TranslatorTest {

    @Test
    public void testTranslator() {
        ListResourceBundle listResourceBundle = new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                return new Object[0][];
            }
        };

        Translator<MyKeys> myKeysTranslator = new Translator<>(listResourceBundle);

        String t = myKeysTranslator.get(STRING1);
        String t2 = new Translator<>(listResourceBundle).get("STRING1");
        String t1 = t(STRING1);
        String s = t(STRING1);
    }
}