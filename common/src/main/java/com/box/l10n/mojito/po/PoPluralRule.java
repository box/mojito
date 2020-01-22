package com.box.l10n.mojito.po;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMultimap;
import com.ibm.icu.text.PluralRules;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides plural form for PO files and mapping to CLDR forms.
 * 
 * Follows rules defined here:
 * https://www.gnu.org/savannah-checkouts/gnu/gettext/manual/html_node/Plural-forms.html
 *
 * @author jeanaurambault
 */
public enum PoPluralRule {
 
    ONE_FORM(
            "nplurals=1; plural=0;",
            PoFormToCLDRForm.ONE_FORM,
            CldrFormsToCopyOnImport.NONE),
    TWO_FORMS_SINGULAR_FOR_ONE(
            "nplurals=2; plural=n != 1;",
            PoFormToCLDRForm.TWO_FORMS,
            CldrFormsToCopyOnImport.NONE),
    TWO_FORMS_SINGULAR_FOR_ZERO_AND_ONE(
            "nplurals=2; plural=n>1;",
            PoFormToCLDRForm.TWO_FORMS,
            CldrFormsToCopyOnImport.NONE),
    THREE_FORMS_SPECIAL_FOR_ZERO(
            "nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n != 0 ? 1 : 2;",
            PoFormToCLDRForm.THREE_FORMS_ONE_FEW_OTHER,
            CldrFormsToCopyOnImport.NONE),
    THREE_FORMS_SPECIAL_FOR_ONE_TWO(
            "nplurals=3; plural=n==1 ? 0 : n==2 ? 1 : 2;",
            PoFormToCLDRForm.THREE_FORMS_ONE_FEW_OTHER,
            CldrFormsToCopyOnImport.FEW_TO_MANY_OTHER),
    THREE_FORMS_SPECIAL_FOR_ENDING_00_2909(
            "nplurals=3; plural=n==1 ? 0 : (n==0 || (n%100 > 0 && n%100 < 20)) ? 1 : 2;",
            PoFormToCLDRForm.THREE_FORMS_ONE_FEW_OTHER,
            CldrFormsToCopyOnImport.NONE),
    THREE_FORMS_SPECIAL_FOR_ENDING_1_29(
            "nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && (n%100<10 || n%100>=20) ? 1 : 2;",
            PoFormToCLDRForm.THREE_FORMS_ONE_FEW_OTHER,
            CldrFormsToCopyOnImport.OTHER_TO_MANY),
    THREE_FORMS_SPECIAL_FOR_ENDING_1_AND_234_EXCEPT_114(
            "nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;",
            PoFormToCLDRForm.THREE_FORMS_ONE_FEW_MANY,
            CldrFormsToCopyOnImport.FEW_TO_OTHER),
    THREE_FORMS_SPECIAL_FOR_ENDING_1_AND_234_EXCEPT_114_ONLY3(
            "nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;",
            PoFormToCLDRForm.THREE_FORMS_ONE_FEW_MANY,
            CldrFormsToCopyOnImport.NONE),
    THREE_FORMS_SPECIAL_FOR_1_2_3_4(
            "nplurals=3; plural=(n==1) ? 0 : (n>=2 && n<=4) ? 1 : 2;",
            PoFormToCLDRForm.THREE_FORMS_ONE_FEW_OTHER,
            CldrFormsToCopyOnImport.OTHER_TO_MANY),
    THREE_FORMS_SPECIAL_FOR_1_AND_ENDING_234(
            "nplurals=3; plural=n==1 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;",
            PoFormToCLDRForm.THREE_FORMS_ONE_FEW_MANY,
            CldrFormsToCopyOnImport.FEW_TO_OTHER),
    THREE_FORMS_SPECIAL_ENDING_020310_AND_1199(
            "nplurals=4; plural=n%100==1 ? 0 : n%100==2 ? 1 : n%100==3 || n%100==4 ? 2 : 3;",
            PoFormToCLDRForm.THREE_FORMS_ONE_FEW_MANY,
            CldrFormsToCopyOnImport.FEW_TO_OTHER),
    SIX_FORMS(
            "nplurals=6; plural=n==0 ? 0 : n==1 ? 1 : n==2 ? 2 : n%100>=3 && n%100<=10 ? 3 : n%100>=11 ? 4 : 5;",
            PoFormToCLDRForm.SIX_FORMS,
            CldrFormsToCopyOnImport.NONE);

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PoPluralRule.class);
    
    final static Map<String, PoPluralRule> mappingForNonDefault = new HashMap<>();

    static {
        mappingForNonDefault.put("pt-BR", PoPluralRule.TWO_FORMS_SINGULAR_FOR_ZERO_AND_ONE);
        mappingForNonDefault.put("fr", PoPluralRule.TWO_FORMS_SINGULAR_FOR_ZERO_AND_ONE);
        mappingForNonDefault.put("lv", PoPluralRule.THREE_FORMS_SPECIAL_FOR_ZERO);
        mappingForNonDefault.put("ga", PoPluralRule.THREE_FORMS_SPECIAL_FOR_ONE_TWO); // incompatible with CLDR?
        mappingForNonDefault.put("ro", PoPluralRule.THREE_FORMS_SPECIAL_FOR_ENDING_00_2909);
        mappingForNonDefault.put("lt", PoPluralRule.THREE_FORMS_SPECIAL_FOR_ENDING_1_29);
        mappingForNonDefault.put("ru", PoPluralRule.THREE_FORMS_SPECIAL_FOR_ENDING_1_AND_234_EXCEPT_114);
        mappingForNonDefault.put("uk", PoPluralRule.THREE_FORMS_SPECIAL_FOR_ENDING_1_AND_234_EXCEPT_114);
        mappingForNonDefault.put("be", PoPluralRule.THREE_FORMS_SPECIAL_FOR_ENDING_1_AND_234_EXCEPT_114);
        mappingForNonDefault.put("sr", PoPluralRule.THREE_FORMS_SPECIAL_FOR_ENDING_1_AND_234_EXCEPT_114_ONLY3);
        mappingForNonDefault.put("hr", PoPluralRule.THREE_FORMS_SPECIAL_FOR_ENDING_1_AND_234_EXCEPT_114_ONLY3);
        mappingForNonDefault.put("cs", PoPluralRule.THREE_FORMS_SPECIAL_FOR_1_2_3_4);
        mappingForNonDefault.put("sk", PoPluralRule.THREE_FORMS_SPECIAL_FOR_1_2_3_4);
        mappingForNonDefault.put("pl", PoPluralRule.THREE_FORMS_SPECIAL_FOR_1_AND_ENDING_234);
        mappingForNonDefault.put("sl", PoPluralRule.THREE_FORMS_SPECIAL_ENDING_020310_AND_1199); // incompatible with CLDR?
        mappingForNonDefault.put("ar", PoPluralRule.SIX_FORMS);
    }
    
    String rule;
    PoFormToCLDRForm poFormToCLDRForm;
    CldrFormsToCopyOnImport formsToCopyOnImport;

    private PoPluralRule(String rule, PoFormToCLDRForm poFormToCLDRForm, CldrFormsToCopyOnImport formsToCopyOnImport) {
        this.rule = rule;
        this.poFormToCLDRForm = poFormToCLDRForm;
        this.formsToCopyOnImport = formsToCopyOnImport;
    }

    public String getRule() {
        return rule;
    }

    public Set<String> getCldrForms() {
        return poFormToCLDRForm.getPoFormToCLDRForm().values();
    }

    public CldrFormsToCopyOnImport getFormsToCopyOnImport() {
        return formsToCopyOnImport;
    }

    public String poFormToCldrForm(String poForm) {
        return poFormToCLDRForm.getPoFormToCLDRForm().get(poForm);
    }

    public String cldrFormToPoForm(String cldrForm) {
        return poFormToCLDRForm.getPoFormToCLDRForm().inverse().get(cldrForm);
    }
    
    static public PoPluralRule fromBcp47Tag(String bcp47tag) {

        logger.debug("Get po plural rules for bcp47tag: {}", bcp47tag);

        PoPluralRule poPluralRule = PoPluralRule.TWO_FORMS_SINGULAR_FOR_ONE;

        Locale forLanguageTag = Locale.forLanguageTag(bcp47tag);

        if (mappingForNonDefault.containsKey(bcp47tag)) {
            poPluralRule = mappingForNonDefault.get(bcp47tag);
        } else if (mappingForNonDefault.containsKey(forLanguageTag.getLanguage())) {
            poPluralRule = mappingForNonDefault.get(forLanguageTag.getLanguage());
        } else if (!Strings.isNullOrEmpty(bcp47tag) && !"und".equals(bcp47tag)) {
            PluralRules cldrPluralRule = PluralRules.forLocale(Locale.forLanguageTag(bcp47tag));
            if (cldrPluralRule.getKeywords().size() == 1) {
                poPluralRule = PoPluralRule.ONE_FORM;
            }
        }

        logger.debug("Po plural rules for bcp47tag is: {}", poPluralRule.toString());
        return poPluralRule;
    }
    

    public enum CldrFormsToCopyOnImport {
        NONE(ImmutableMultimap.<String, String>of()),
        OTHER_TO_MANY(ImmutableMultimap.of("other", "many")),
        FEW_TO_OTHER(ImmutableMultimap.of("many", "other")),
        FEW_TO_MANY_OTHER(ImmutableMultimap.of("few", "many", "few", "other"));

        ImmutableMultimap<String, String> formMap;

        private CldrFormsToCopyOnImport(ImmutableMultimap<String, String> formMap) {
            this.formMap = formMap;
        }

        public ImmutableMultimap<String, String> getFormMap() {
            return formMap;
        }
    }

    public enum PoFormToCLDRForm {
        ONE_FORM(ImmutableBiMap.of("0", "other")),
        TWO_FORMS(ImmutableBiMap.of(
                "0", "one",
                "1", "other")),
        THREE_FORMS_ONE_FEW_MANY(ImmutableBiMap.of(
                "0", "one",
                "1", "few",
                "2", "many"
        )),
        THREE_FORMS_ONE_FEW_OTHER(ImmutableBiMap.of(
                "0", "one",
                "1", "few",
                "2", "other"
        )),
        THREE_FORMS_ONE_TWO_FEW(ImmutableBiMap.of(
                "0", "one",
                "1", "two",
                "2", "few"
        )),
        SIX_FORMS(ImmutableBiMap.<String, String>builder().
                put("0", "zero").
                put("1", "one").
                put("2", "two").
                put("3", "few").
                put("4", "many").
                put("5", "other").build());

        ImmutableBiMap<String, String> poFormToCLDRForm;

        private PoFormToCLDRForm(ImmutableBiMap<String, String> poFormToCLDRForm) {
            this.poFormToCLDRForm = poFormToCLDRForm;
        }

        public ImmutableBiMap<String, String> getPoFormToCLDRForm() {
            return poFormToCLDRForm;
        }
    }
}
