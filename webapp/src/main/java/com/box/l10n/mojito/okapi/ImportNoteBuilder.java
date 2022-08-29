package com.box.l10n.mojito.okapi;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple builder to create an import note that contains messages with severity type about the
 * import process.
 *
 * @author jaurambault
 */
public class ImportNoteBuilder {

  public static class ImportNoteMessage {

    public enum Type {
      INFO,
      WARNING,
      ERROR,
    }

    Type type;
    String text;

    public ImportNoteMessage(Type type, String text) {
      this.type = type;
      this.text = text;
    }
  }

  boolean needsReview = false;
  boolean mustReview = false;

  List<ImportNoteMessage> messages = new ArrayList<>();

  /**
   * Adds a message (if not null) to the builder.
   *
   * @param type type of message
   * @param message message to be added. If {@code null} nothing is added
   * @return the builder
   */
  ImportNoteBuilder addMessage(ImportNoteMessage.Type type, String message) {
    if (!Strings.isNullOrEmpty(message)) {
      messages.add(new ImportNoteMessage(type, message));
    }

    return this;
  }

  public ImportNoteBuilder addInfo(String message) {
    return addMessage(ImportNoteMessage.Type.INFO, message);
  }

  public ImportNoteBuilder addWarning(String message) {
    return addMessage(ImportNoteMessage.Type.WARNING, message);
  }

  public ImportNoteBuilder addError(String message) {
    return addMessage(ImportNoteMessage.Type.ERROR, message);
  }

  public ImportNoteBuilder setMustReview(boolean mustReview) {
    this.mustReview = mustReview;
    return this;
  }

  public ImportNoteBuilder setNeedsReview(boolean needsReview) {
    this.needsReview = needsReview;
    return this;
  }

  @Override
  public String toString() {

    StringBuilder stringBuilder = new StringBuilder();

    if (mustReview) {
      stringBuilder.append("MUST REVIEW");
    } else if (needsReview) {
      stringBuilder.append("NEEDS REVIEW");
    } else {
      stringBuilder.append("OK");
    }

    for (ImportNoteMessage message : messages) {
      stringBuilder.append("\n[").append(message.type).append("] ").append(message.text);
    }

    return stringBuilder.toString();
  }
}
