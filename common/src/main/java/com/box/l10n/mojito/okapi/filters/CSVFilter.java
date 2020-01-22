package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.filters.table.csv.CommaSeparatedValuesFilter;
import org.springframework.util.StringUtils;

/**
 * @author aloison
 */
public class CSVFilter extends CommaSeparatedValuesFilter {

    public static final String FILTER_NAME = "okf_table_csv@mojito";
    public static final String FILTER_CONFIG_ID = "okf_table_csv@mojito";

    public CSVFilter() {
        super();

        
        setName(FILTER_NAME);
        addConfiguration(
                false, // Inherit configurations from CommaSeparatedValuesFilter
                FILTER_CONFIG_ID,
                "Table (Comma-Separated Values)",
                "Comma-separated values, optional header with field names.",
                "okf_table_csv_mojito.fprm"
        );
    }

    /**
     * Overriding to trim comment/note
     *
     * Trimming is needed because the comment in the CSV is often escaped with quotes, but the okapi filter
     * does not trim quotes from comment automatically.
     *
     * @param textUnit
     * @return
     */
    @Override
    protected boolean processTU(ITextUnit textUnit) {

        Property note = textUnit.getProperty(Property.NOTE);
        if (note != null) {
            String comments = note.toString();
            char quote = "\"".charAt(0);
            comments = StringUtils.trimLeadingCharacter(comments, quote);
            comments = StringUtils.trimTrailingCharacter(comments, quote);
            note.setValue(comments);
        }

        return super.processTU(textUnit);
    }

}
