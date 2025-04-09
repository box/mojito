package com.box.l10n.mojito.service.converter;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.Arrays;
import java.util.stream.Collectors;
import joptsimple.internal.Strings;

public class TextUnitConverterGettext implements TextUnitConverter {

  private final String COMMENT_SKELETON = "#. ${##}";
  private final String CONTEXT_SKELETON = "msgctxt \"${##}\"";
  private final String MSGID_SKELETON = "msgid \"${##}\"";
  private final String MSGID_PLURAL = "msgid_plural \"${##}\"";
  private final String MSGSTR = "msgstr \"\"";
  private final String CONTEXT_SEPARATOR = " --- ";

  private final String lineBreak;

  public TextUnitConverterGettext(String lineBreak) {
    this.lineBreak = lineBreak;
  }

  @Override
  public String convert(TextUnitDTO textUnitDTO) {
    boolean hasContext = textUnitDTO.getName().contains(CONTEXT_SEPARATOR);

    StringBuilder sb = new StringBuilder();

    if (!Strings.isNullOrEmpty(textUnitDTO.getComment())) sb.append(getComment(textUnitDTO));
    if (hasContext) sb.append(getContext(textUnitDTO)).append(lineBreak);

    if (textUnitDTO.getPluralForm() != null && textUnitDTO.getPluralForm().equals("other")) {
      String singular = textUnitDTO.getName().split(" _other")[0];
      String plural = textUnitDTO.getSource();

      if (hasContext) {
        singular = singular.split(CONTEXT_SEPARATOR)[0];
      }

      sb.append(replace(MSGID_SKELETON, singular, false)).append(lineBreak);
      sb.append(replace(MSGID_PLURAL, plural, true)).append(lineBreak);
      sb.append("msgstr[0] \"\"").append(lineBreak);
      sb.append("msgstr[1] \"\"").append(lineBreak + lineBreak);
    } else {
      sb.append(getMsgId(textUnitDTO)).append(lineBreak);
      sb.append(MSGSTR).append(lineBreak + lineBreak);
    }

    return sb.toString();
  }

  private String getComment(TextUnitDTO textUnitDTO) {
    // Break down comment by new line, make sure each line starts with #.
    String[] comments = textUnitDTO.getComment().split(lineBreak);
    return Arrays.stream(comments)
            .map(comment -> replace(COMMENT_SKELETON, comment, false))
            .collect(Collectors.joining(lineBreak))
        + lineBreak;
  }

  private String getContext(TextUnitDTO textUnitDTO) {
    String context = textUnitDTO.getName().split(CONTEXT_SEPARATOR)[1];
    if (textUnitDTO.getPluralForm() != null) {
      context = context.split(" _" + textUnitDTO.getPluralForm())[0];
    }
    return replace(CONTEXT_SKELETON, context, false);
  }

  private String getMsgId(TextUnitDTO textUnitDTO) {
    return replace(MSGID_SKELETON, textUnitDTO.getSource(), true);
  }

  private String replace(String skeleton, String replacement, boolean escape) {
    String repString = escape ? escape(replacement) : replacement;
    return skeleton.replace("${##}", repString);
  }

  private String escape(String toEscape) {
    return toEscape
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n");
  }
}
