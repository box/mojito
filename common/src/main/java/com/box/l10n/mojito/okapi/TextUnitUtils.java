package com.box.l10n.mojito.okapi;

import java.util.Objects;
import net.sf.okapi.common.annotation.XLIFFNoteAnnotation;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** @author jaurambault */
@Component
public class TextUnitUtils {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(TextUnitUtils.class);

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
   * Gets the source container as a string
   *
   * @param textUnit
   * @return
   */
  public String getSourceAsString(ITextUnit textUnit) {
    return textUnit.getSource().toString();
  }

  /**
   * Replace the source container with the given string
   *
   * @param textUnit
   * @param newSource
   */
  public void replaceSourceString(TextUnit textUnit, String newSource) {
    TextContainer source = new TextContainer(newSource);
    textUnit.setSource(source);
  }

  /**
   * Computes a MD5 hash for a {@link TextUnit}.
   *
   * @param name the text unit name
   * @param content the text unit content
   * @param comment the text unit comment
   * @return the MD5 hash in Hex
   */
  public String computeTextUnitMD5(String name, String content, String comment) {
    return DigestUtils.md5Hex(name + content + comment);
  }
}
