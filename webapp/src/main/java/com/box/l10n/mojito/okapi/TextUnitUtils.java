package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.tm.ImportExportNote;
import java.io.IOException;
import java.util.Objects;
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
public class TextUnitUtils {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TextUnitUtils.class);

    @Autowired
    ObjectMapper objectMapper;

    /**
     * Gets the note from a {@link ITextUnit}.
     *
     * @param textUnit that contains the note
     * @return the note or {@code null} if none
     */
    public String getNote(ITextUnit textUnit) {

        String note = null;

        if (textUnit != null) {

            XLIFFNoteAnnotation xliffNoteAnnotation = textUnit.getAnnotation(XLIFFNoteAnnotation.class);

            if (xliffNoteAnnotation == null) {
                note = Objects.toString(textUnit.getProperty(Property.NOTE), null);
            } else {
                note = xliffNoteAnnotation.getNote(0).getNoteText();
            }
        }

        return note;
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

        String note = getNote(textUnit);

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

        textUnit.setProperty(new Property(Property.NOTE, importExportNoteStr));

        XLIFFNoteAnnotation xliffNoteAnnotation = textUnit.getAnnotation(XLIFFNoteAnnotation.class);

        if (xliffNoteAnnotation == null) {
            xliffNoteAnnotation = new XLIFFNoteAnnotation();
            textUnit.setAnnotation(xliffNoteAnnotation);
        }

        xliffNoteAnnotation.add(new XLIFFNote(importExportNoteStr));
    }

}
