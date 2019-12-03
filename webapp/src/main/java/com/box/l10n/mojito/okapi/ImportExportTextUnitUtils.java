package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.tm.ImportExportNote;
import java.io.IOException;

import net.sf.okapi.common.annotation.XLIFFNote;
import net.sf.okapi.common.annotation.XLIFFNoteAnnotation;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author jaurambault
 */
@Component
public class ImportExportTextUnitUtils {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ImportExportTextUnitUtils.class);

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TextUnitUtils textUnitUtils;

    /**
     * Set the note to a {@link ITextUnit}.
     *
     * @param textUnit to link with note
     * @param note note to set
     */
    public void setNote(ITextUnit textUnit, String note) {

        if (textUnit != null) {
            textUnit.setProperty(new Property(Property.NOTE, note));
        }
    }

    /**
     * Gets an {@link ImportExportNote} based on the content of {@link TextUnit}
     * note.
     * <p>
     * The note might not contain JSON or valid JSON. In that case the note
     * content is set in {@link ImportExportNote#sourceComment}. This allows to
     * import files that don't contain JSON, for backward compatibility or just
     * file simplicity.
     *
     * @param textUnit contains the note that needs to be processed
     * @return the created {@link ImportExportNote}
     */
    public ImportExportNote getImportExportNote(ITextUnit textUnit) {

        ImportExportNote importExportNote = new ImportExportNote();

        String note = textUnitUtils.getNote(textUnit);

        if (note != null) {
            try {
                importExportNote = objectMapper.readValue(note, ImportExportNote.class);
            } catch (IOException ioe) {
                logger.debug("Couldn't convert the note into ImportExportNote, set the whole string as comment");
                importExportNote.setSourceComment(note);
            }
        }

        return importExportNote;
    }

    /**
     * Converts the {@link ImportExportNote} to JSON and assigned it to the
     * {@link ITextUnit}.
     *
     * @param textUnit to be updated with the note
     * @param importExportNote to be converted into a JSON string for the note
     */
    public void setImportExportNote(ITextUnit textUnit, ImportExportNote importExportNote) {

        String importExportNoteStr = objectMapper.writeValueAsStringUnsafe(importExportNote);

        XLIFFNoteAnnotation xliffNoteAnnotation = textUnit.getAnnotation(XLIFFNoteAnnotation.class);

        if (xliffNoteAnnotation == null) {
            xliffNoteAnnotation = new XLIFFNoteAnnotation();
            textUnit.setAnnotation(xliffNoteAnnotation);
        } else {
            setNote(textUnit, importExportNoteStr);
        }

        xliffNoteAnnotation.add(new XLIFFNote(importExportNoteStr));
    }
}
