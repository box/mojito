package com.box.l10n.mojito.test;

import com.google.common.base.Function;

/**
 *
 * @author jaurambault
 */
public class XliffUtils {

    /**
     * Replaces content that changes from one test run to another (mostly db
     * ids, dates, etc) in the Xliff to allow simple comparison of the xliff
     * content.
     *
     * @param xliffContent original content
     * @return xliff where the variable content was replaced with "replaced-id"
     */
    public static String replaceXliffVariableContent(String xliffContent) {

        return xliffContent.
                replaceFirst("file original=\".*?\"", "file original=\"replaced-original\"").
                replaceAll("id=\".*?\"", "id=\"replaced-id\"").
                replaceAll("tuv id: \\d+", "tuv id: replaced-id");
    }

    /**
     * Gets a function that replaces created data in the XLIFF content.
     *
     * @return the function that replaces created data in the XLIFF content.
     */
    public static Function<String, String> replaceCreatedDateFunction() {

        return new Function<String, String>() {

            @Override
            public String apply(String input) {
                return input.
                        replaceAll("\"createdDate\":\\d+,?", "").
                        replaceAll(",\\}", "}");
            }
        };
    }

    
    /**
     * Gets a function that call {@link #replaceXliffVariableContent(java.lang.String) }
     * to modify content.
     *
     * @return the function that replaces the XLIFF content.
     */
    public static Function<String, String> replaceXliffVariableContentFunction() {

        return new Function<String, String>() {

            @Override
            public String apply(String input) {
                return XliffUtils.replaceXliffVariableContent(input);
            }
        };
    }

    
    /**
     * Localizes an Xliff file by replacing original targets by target where the
     * content has been prepended with a prefix and the locale.
     *
     * @param xliffContent orginal to localize
     * @param localizedTargetPrefix prefix to prepend in targets
     * @return the localized XLIFF
     */
    public static String localizeTarget(String xliffContent, String localizedTargetPrefix) {
        return xliffContent.replaceAll("<target xml:lang=\"(.*?)\" state=\"(.*?)\">(.*?)</target>", "<target xml:lang=\"$1\" state=\"$2\">" + localizedTargetPrefix + " - $3 $1</target>");
    }

    /**
     * Replaces the state of all targets in the XLIFF.
     *
     * Note that the regex acutally replaces the state without looking at the
     * target for context so this assumes the state attribute is not used in
     * another context (which should be fine for xliff standard, if not the
     * regex should be made more specific)
     *
     * @param xliffContent
     * @param state
     * @return
     */
    public static String replaceTargetState(String xliffContent, String state) {
        return xliffContent.replaceAll("state=\"(.*?)\"", "state=\"" + state + "\"");
    }

}
