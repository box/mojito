package com.box.l10n.mojito.factory;

import com.box.l10n.mojito.okapi.XliffState;
import java.util.List;
import java.util.Objects;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.NoteAnnotation;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import org.springframework.stereotype.Component;

/** @author aloison */
@Component
public class XliffDataFactory {

  /**
   * Creates a textUnit to be used later to generate a XLIFF
   *
   * @param id
   * @param name
   * @param source
   * @param note Can be {@code null}
   * @return
   */
  public TextUnit createTextUnit(Long id, String name, String source, String note) {
    return createTextUnit(id, name, source, note, null, null, null);
  }

  /**
   * Creates a textUnit to be used later to generate a XLIFF
   *
   * @param id
   * @param name
   * @param source
   * @param note Can be {@code null}
   * @param target Can be {@code null}
   * @param targetBcp47Tag Can be {@code null}
   * @param state
   * @return
   */
  public TextUnit createTextUnit(
      Long id,
      String name,
      String source,
      String note,
      String target,
      String targetBcp47Tag,
      XliffState state) {

    TextUnit textUnit = new TextUnit(id.toString(), source);
    textUnit.setName(name);

    if (note != null) {
      textUnit.setSourceProperty(new Property(NoteAnnotation.LOC_NOTE, note));
    }

    if (target != null && targetBcp47Tag != null) {
      LocaleId localeId = LocaleId.fromBCP47(targetBcp47Tag);
      textUnit.setTarget(localeId, new TextContainer(target));
      if (state != null) {
        textUnit.setProperty(new Property("state", state.toString()));
      }
    }

    return textUnit;
  }

  /**
   * Generates a source XLIFF from the list of given {@link TextUnit}s
   *
   * @param textUnits
   * @return
   */
  public String generateSourceXliff(List<TextUnit> textUnits) {
    return generateXliff(textUnits, null);
  }

  /**
   * Generates a target XLIFF from the list of given {@link TextUnit}s
   *
   * @param textUnits
   * @param targetBcp47Tag
   * @return
   */
  public String generateTargetXliff(List<TextUnit> textUnits, String targetBcp47Tag) {
    return generateXliff(textUnits, targetBcp47Tag);
  }

  /**
   * @param textUnits
   * @param targetBcp47Tag Can be {@code null}
   * @return
   */
  private String generateXliff(List<TextUnit> textUnits, String targetBcp47Tag) {

    boolean isTargetXliff = (targetBcp47Tag != null);

    String xliff = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    xliff += "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" version=\"2.0\">\n";

    if (targetBcp47Tag == null) {
      xliff += " <file original=\"\" source-language=\"en\" datatype=\"x-undefined\">\n";
    } else {
      xliff +=
          " <file original=\"\" source-language=\"en\" datatype=\"x-undefined\" target-language=\""
              + targetBcp47Tag
              + "\">\n";
    }

    xliff += "  <body>\n";

    for (TextUnit textUnit : textUnits) {
      xliff +=
          "   <trans-unit id=\""
              + textUnit.getId()
              + "\" resname=\""
              + textUnit.getName()
              + "\">\n";
      xliff += "    <source>" + textUnit.getSource().toString() + "</source>\n";

      if (targetBcp47Tag != null) {
        xliff += "    <target xml:lang=\"" + targetBcp47Tag + "\"";

        Property state = textUnit.getProperty("state");
        if (state != null) {
          xliff += " state=\"" + state.getValue() + "\" ";
        }

        xliff +=
            ">" + textUnit.getTarget(LocaleId.fromBCP47(targetBcp47Tag)).toString() + "</target>\n";
      }

      String note = Objects.toString(textUnit.getSourceProperty(NoteAnnotation.LOC_NOTE), null);
      if (note != null) {
        if (!isTargetXliff) {
          // Okapi does not indent code properly...
          xliff += "    ";
        }
        xliff += "<note>" + note + "</note>\n";
      }

      xliff += "   </trans-unit>\n";
    }

    xliff += "  </body>\n" + " </file>\n" + "</xliff>";

    return xliff;
  }
}
