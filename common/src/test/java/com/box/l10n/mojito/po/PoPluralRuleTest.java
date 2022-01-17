package com.box.l10n.mojito.po;

import com.ibm.icu.text.PluralRules;
import java.util.Arrays;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeanaurambault
 */
public class PoPluralRuleTest {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PoPluralRuleTest.class);


    @Test
    public void testArabic() {
        Assert.assertEquals("nplurals=6; plural=n==0 ? 0 : n==1 ? 1 : n==2 ? 2 : n%100>=3 && n%100<=10 ? 3 : n%100>=11 ? 4 : 5;", PoPluralRule.fromBcp47Tag("ar").getRule());
    }

    @Test
    public void testEnglish() {
        Assert.assertEquals("nplurals=2; plural=n != 1;", PoPluralRule.fromBcp47Tag("en").getRule());
    }

    @Test
    public void testGerman() {
        Assert.assertEquals("nplurals=2; plural=n != 1;", PoPluralRule.fromBcp47Tag("en").getRule());
    }

    @Test
    public void testFrench() {
        Assert.assertEquals("nplurals=2; plural=n>1;", PoPluralRule.fromBcp47Tag("fr").getRule());
    }

    @Test
    public void testLatavian() {
        Assert.assertEquals("nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n != 0 ? 1 : 2;", PoPluralRule.fromBcp47Tag("lv").getRule());
    }

    @Test
    public void testKorean() {
        Assert.assertEquals("nplurals=1; plural=0;", PoPluralRule.fromBcp47Tag("ko").getRule());
    }

    @Test
    public void testGaelic() {
        Assert.assertEquals("nplurals=3; plural=n==1 ? 0 : n==2 ? 1 : 2;", PoPluralRule.fromBcp47Tag("ga").getRule());
    }

    @Test
    public void testRomanian() {
        Assert.assertEquals("nplurals=3; plural=n==1 ? 0 : (n==0 || (n%100 > 0 && n%100 < 20)) ? 1 : 2;", PoPluralRule.fromBcp47Tag("ro").getRule());
    }

    @Test
    public void testLithuanian() {
        Assert.assertEquals("nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && (n%100<10 || n%100>=20) ? 1 : 2;", PoPluralRule.fromBcp47Tag("lt").getRule());
    }

    @Test
    public void testRussian() {
        Assert.assertEquals("nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;", PoPluralRule.fromBcp47Tag("ru").getRule());
    }

    @Test
    public void testCzech() {
        Assert.assertEquals("nplurals=3; plural=(n==1) ? 0 : (n>=2 && n<=4) ? 1 : 2;", PoPluralRule.fromBcp47Tag("cs").getRule());
    }

    @Test
    public void testPolish() {
        Assert.assertEquals("nplurals=3; plural=n==1 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;", PoPluralRule.fromBcp47Tag("pl").getRule());
    }

    @Test
    public void testSlovenian() {
        Assert.assertEquals("nplurals=4; plural=n%100==1 ? 0 : n%100==2 ? 1 : n%100==3 || n%100==4 ? 2 : 3;", PoPluralRule.fromBcp47Tag("sl").getRule());
    }

    @Test
    public void testPtBR() {
        Assert.assertEquals(PoPluralRule.TWO_FORMS_SINGULAR_FOR_ZERO_AND_ONE, PoPluralRule.fromBcp47Tag("pt-BR"));

    }

    @Test
    public void testPt() {
        Assert.assertEquals(PoPluralRule.TWO_FORMS_SINGULAR_FOR_ONE, PoPluralRule.fromBcp47Tag("pt"));

    }

    @Test
    public void testFrFR() {
        Assert.assertEquals(PoPluralRule.TWO_FORMS_SINGULAR_FOR_ZERO_AND_ONE, PoPluralRule.fromBcp47Tag("fr-FR"));
    }

    @Test
    public void testidID() {
        PoPluralRule rulesForBcp47Tag = PoPluralRule.fromBcp47Tag("id-ID");
        Assert.assertEquals(PoPluralRule.ONE_FORM, rulesForBcp47Tag);
    }

    @Test
    public void testGa() {
        PoPluralRule rulesForBcp47Tag = PoPluralRule.fromBcp47Tag("ga");
        Assert.assertEquals(PoPluralRule.THREE_FORMS_SPECIAL_FOR_ONE_TWO, rulesForBcp47Tag);
        Assert.assertEquals(Arrays.asList("many", "other"), rulesForBcp47Tag.getFormsToCopyOnImport().getFormMap().get("few"));

    }

    @Test
    public void testUnd() {
        PoPluralRule rulesForBcp47Tag = PoPluralRule.fromBcp47Tag("und");
        Assert.assertEquals(PoPluralRule.TWO_FORMS_SINGULAR_FOR_ONE, rulesForBcp47Tag);
    }

    @Test
    public void testHebrew() {
        PoPluralRule rulesForBcp47Tag = PoPluralRule.fromBcp47Tag("he");
        Assert.assertEquals(PoPluralRule.FOUR_FORMS_FRACTIONAL_DIGITS_OTHER, rulesForBcp47Tag);
    }

    @Test(expected = NullPointerException.class)
    public void testNull() {
        PoPluralRule.fromBcp47Tag(null);
    }

    @Test
    public void testCopyFormSize() {
        for (String bcp47tag : PoPluralRule.mappingForNonDefault.keySet()) {
            PluralRules cldrPluralRule = PluralRules.forLocale(Locale.forLanguageTag(bcp47tag));
             int cldrSize = cldrPluralRule.getKeywords().size();
             int poSize = PoPluralRule.fromBcp47Tag(bcp47tag).getCldrForms().size();
             int copySize = PoPluralRule.fromBcp47Tag(bcp47tag).getFormsToCopyOnImport().getFormMap().values().size();
            logger.debug("{} --> cldr size: {}, po size: {}, copy size: {}", 
                    bcp47tag, cldrSize, poSize, copySize);
            Assert.assertEquals(copySize, cldrSize - poSize);
        }
    }

}
