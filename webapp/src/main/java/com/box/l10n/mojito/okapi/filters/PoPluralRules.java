package com.box.l10n.mojito.okapi.filters;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * See:
 * https://www.gnu.org/savannah-checkouts/gnu/gettext/manual/html_node/Plural-forms.html
 *
 * @author jeanaurambault
 */
@Component
public class PoPluralRules {

    public enum Rules {
        ONE_FORM("nplurals=1; plural=0;"),
        TWO_FORMS_SINGULAR_FOR_ONE("nplurals=2; plural=n != 1;"),
        TWO_FORMS_SINGULAR_FOR_ZERO_AND_ONE("nplurals=2; plural=n>1;"),
        THREE_FORMS_SPECIAL_FOR_ZERO("nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n != 0 ? 1 : 2;"),
        THREE_FORMS_SPECIAL_FOR_ONE_TWO("nplurals=3; plural=n==1 ? 0 : n==2 ? 1 : 2;"),
        THREE_FORMS_SPECIAL_FOR_ENDING_00_2909("nplurals=3; plural=n==1 ? 0 : (n==0 || (n%100 > 0 && n%100 < 20)) ? 1 : 2;"),
        THREE_FORMS_SPECIAL_FOR_ENDING_1_29("nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && (n%100<10 || n%100>=20) ? 1 : 2;"),
        THREE_FORMS_SPECIAL_FOR_ENDING_1_AND_234_EXCEPT_114("nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;"),
        THREE_FORMS_SPECIAL_FOR_1_2_3_4("nplurals=3; plural=(n==1) ? 0 : (n>=2 && n<=4) ? 1 : 2;"),
        THREE_FORMS_SPECIAL_FOR_1_AND_ENDING_234("nplurals=3; plural=n==1 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;"),
        THREE_FORMS_SPECIAL_ENDING_020310_AND_1199("nplurals=4; plural=n%100==1 ? 0 : n%100==2 ? 1 : n%100==3 || n%100==4 ? 2 : 3;"),
        SIX_FORMS("nplurals=6; plural=n==0 ? 0 : n==1 ? 1 : n==2 ? 2 : n%100>=3 && n%100<=10 ? 3 : n%100>=11 ? 4 : 5;");

        String rule;

        private Rules(String rule) {
            this.rule = rule;
        }

        public String getRule() {
            return rule;
        }
       
    }

    Map<String, Rules> mappingForNonDefault = new HashMap<>();

    public PoPluralRules() {
        mappingForNonDefault.put("ja", Rules.ONE_FORM);
        mappingForNonDefault.put("vi", Rules.ONE_FORM);
        mappingForNonDefault.put("ko", Rules.ONE_FORM);
        mappingForNonDefault.put("th", Rules.ONE_FORM);        
        mappingForNonDefault.put("pt-BR", PoPluralRules.Rules.TWO_FORMS_SINGULAR_FOR_ZERO_AND_ONE);
        mappingForNonDefault.put("fr", PoPluralRules.Rules.TWO_FORMS_SINGULAR_FOR_ZERO_AND_ONE);
        mappingForNonDefault.put("lv", PoPluralRules.Rules.THREE_FORMS_SPECIAL_FOR_ZERO);
        mappingForNonDefault.put("ga", PoPluralRules.Rules.THREE_FORMS_SPECIAL_FOR_ONE_TWO);
        mappingForNonDefault.put("ro", PoPluralRules.Rules.THREE_FORMS_SPECIAL_FOR_ENDING_00_2909);
        mappingForNonDefault.put("lt", PoPluralRules.Rules.THREE_FORMS_SPECIAL_FOR_ENDING_1_29);
        mappingForNonDefault.put("ru", PoPluralRules.Rules.THREE_FORMS_SPECIAL_FOR_ENDING_1_AND_234_EXCEPT_114);
        mappingForNonDefault.put("uk", PoPluralRules.Rules.THREE_FORMS_SPECIAL_FOR_ENDING_1_AND_234_EXCEPT_114);
        mappingForNonDefault.put("be", PoPluralRules.Rules.THREE_FORMS_SPECIAL_FOR_ENDING_1_AND_234_EXCEPT_114);
        mappingForNonDefault.put("hr", PoPluralRules.Rules.THREE_FORMS_SPECIAL_FOR_ENDING_1_AND_234_EXCEPT_114);
        mappingForNonDefault.put("sr", PoPluralRules.Rules.THREE_FORMS_SPECIAL_FOR_ENDING_1_AND_234_EXCEPT_114);
        mappingForNonDefault.put("cs", PoPluralRules.Rules.THREE_FORMS_SPECIAL_FOR_1_2_3_4);
        mappingForNonDefault.put("sk", PoPluralRules.Rules.THREE_FORMS_SPECIAL_FOR_1_2_3_4);
        mappingForNonDefault.put("pl", PoPluralRules.Rules.THREE_FORMS_SPECIAL_FOR_1_AND_ENDING_234);
        mappingForNonDefault.put("sl", PoPluralRules.Rules.THREE_FORMS_SPECIAL_ENDING_020310_AND_1199);
        mappingForNonDefault.put("ar", PoPluralRules.Rules.SIX_FORMS);
    }
    
    
    public Rules getRulesForBcp47Tag(String bcp47tag) {
        
        Rules rule = Rules.TWO_FORMS_SINGULAR_FOR_ONE;
        
        Locale forLanguageTag = Locale.forLanguageTag(bcp47tag);
        
        if (mappingForNonDefault.containsKey(bcp47tag)) {
            rule = mappingForNonDefault.get(bcp47tag);
        } else if (mappingForNonDefault.containsKey(forLanguageTag.getLanguage())) {
            rule = mappingForNonDefault.get(forLanguageTag.getLanguage());
        }
        
        return rule;
    }
    
}
