package com.box.l10n.mojito.service.tm;

/**
 *
 * @author jaurambault
 */
public class UpdateTMWithXLIFFResult {
    
    /**
     * Imported XLIFF that has been enriched with meta information during the
     * import process.
     */
    String xliffContent;
    
    /**
     * An optional comment regarding the whole XLIFF file. Usually contains
     * message indicating there was an issue during import and that the file
     * needs to be reviewed.
     */
    String comment;

    public String getXliffContent() {
        return xliffContent;
    }

    public void setXliffContent(String xliffContent) {
        this.xliffContent = xliffContent;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
