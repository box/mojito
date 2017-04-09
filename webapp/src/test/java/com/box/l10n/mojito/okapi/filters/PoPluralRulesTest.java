package com.box.l10n.mojito.okapi.filters;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jeanaurambault
 */
public class PoPluralRulesTest {

    PoPluralRules poPluralRules = new PoPluralRules();

    @Test
    public void testArabic() {
        Assert.assertEquals("nplurals=6; plural=n==0 ? 0 : n==1 ? 1 : n==2 ? 2 : n%100>=3 && n%100<=10 ? 3 : n%100>=11 ? 4 : 5;", poPluralRules.getRulesForBcp47Tag("ar").getRule());
    }

    @Test
    public void testEnglish() {
        Assert.assertEquals("nplurals=2; plural=n != 1;", poPluralRules.getRulesForBcp47Tag("en").getRule());
    }

    @Test
    public void testGerman() {
        Assert.assertEquals("nplurals=2; plural=n != 1;", poPluralRules.getRulesForBcp47Tag("en").getRule());
    }

    @Test
    public void testFrench() {
        Assert.assertEquals("nplurals=2; plural=n>1;", poPluralRules.getRulesForBcp47Tag("fr").getRule());
    }

    @Test
    public void testLatavian() {
        Assert.assertEquals("nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n != 0 ? 1 : 2;", poPluralRules.getRulesForBcp47Tag("lv").getRule());
    }

    @Test
    public void testKorean() {
        Assert.assertEquals("nplurals=1; plural=0;", poPluralRules.getRulesForBcp47Tag("ko").getRule());
    }

    @Test
    public void testGaelic() {
        Assert.assertEquals("nplurals=3; plural=n==1 ? 0 : n==2 ? 1 : 2;", poPluralRules.getRulesForBcp47Tag("ga").getRule());
    }

    @Test
    public void testRomanian() {
        Assert.assertEquals("nplurals=3; plural=n==1 ? 0 : (n==0 || (n%100 > 0 && n%100 < 20)) ? 1 : 2;", poPluralRules.getRulesForBcp47Tag("ro").getRule());
    }

    @Test
    public void testLithuanian() {
        Assert.assertEquals("nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && (n%100<10 || n%100>=20) ? 1 : 2;", poPluralRules.getRulesForBcp47Tag("lt").getRule());
    }

    @Test
    public void testRussian() {
        Assert.assertEquals("nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;", poPluralRules.getRulesForBcp47Tag("ru").getRule());
    }

    @Test
    public void testCzech() {
        Assert.assertEquals("nplurals=3; plural=(n==1) ? 0 : (n>=2 && n<=4) ? 1 : 2;", poPluralRules.getRulesForBcp47Tag("cs").getRule());
    }

    @Test
    public void testPolish() {
        Assert.assertEquals("nplurals=3; plural=n==1 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;", poPluralRules.getRulesForBcp47Tag("pl").getRule());
    }

    @Test
    public void testSlovenian() {
        Assert.assertEquals("nplurals=4; plural=n%100==1 ? 0 : n%100==2 ? 1 : n%100==3 || n%100==4 ? 2 : 3;", poPluralRules.getRulesForBcp47Tag("sl").getRule());
    }

    @Test
    public void testPtBR() {
        Assert.assertEquals(PoPluralRules.Rules.TWO_FORMS_SINGULAR_FOR_ZERO_AND_ONE, poPluralRules.getRulesForBcp47Tag("pt-BR"));

    }

    @Test
    public void testPt() {
        Assert.assertEquals(PoPluralRules.Rules.TWO_FORMS_SINGULAR_FOR_ONE, poPluralRules.getRulesForBcp47Tag("pt"));

    }

    @Test
    public void testFrFR() {
        Assert.assertEquals(PoPluralRules.Rules.TWO_FORMS_SINGULAR_FOR_ZERO_AND_ONE, poPluralRules.getRulesForBcp47Tag("fr-FR"));
    }

}
