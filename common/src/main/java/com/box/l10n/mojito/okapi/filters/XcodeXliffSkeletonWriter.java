package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.filters.xliff.Parameters;
import net.sf.okapi.filters.xliff.XLIFFSkeletonWriter;

/**
 *
 * @author jyi
 */
public class XcodeXliffSkeletonWriter extends XLIFFSkeletonWriter {

    public XcodeXliffSkeletonWriter(Parameters params) {
        super(params);
    }

    @Override
    protected String getString(ITextUnit tu,
            LocaleId locToUse,
            EncoderContext context) {
        String tuString = super.getString(tu, locToUse, context);
        return encodeTransUnitId(tuString);
    }

    private String encodeTransUnitId(String tuString) {
        String tuIdStart = "<trans-unit id=\"";
        int start = tuString.indexOf(tuIdStart);
        if (start < 0) {
            return tuString;
        } else {
            start += tuIdStart.length();
            int end = tuString.indexOf("\"", start);

            String tuIdString = tuString.substring(start, end);
            tuIdString = tuIdString.replaceAll(">", "&gt;");
            tuIdString = tuIdString.replaceAll("\n", "&#10;");

            StringBuilder encoded = new StringBuilder();
            encoded.append(tuString.substring(0, start));
            encoded.append(tuIdString);
            encoded.append(tuString.substring(end, tuString.length()));

            return encoded.toString();
        }
    }
}
