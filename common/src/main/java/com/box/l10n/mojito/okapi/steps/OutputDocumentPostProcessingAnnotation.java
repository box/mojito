package com.box.l10n.mojito.okapi.steps;

import net.sf.okapi.common.annotation.IAnnotation;

public class OutputDocumentPostProcessingAnnotation implements IAnnotation {
  OutputDocumentPostProcessor outputDocumentPostProcessor;

  public OutputDocumentPostProcessingAnnotation(
      OutputDocumentPostProcessor outputDocumentPostProcessor) {
    this.outputDocumentPostProcessor = outputDocumentPostProcessor;
  }

  public OutputDocumentPostProcessor getOutputDocumentPostProcessor() {
    return outputDocumentPostProcessor;
  }

  public interface OutputDocumentPostProcessor {
    String execute(String content);

    boolean hasRemoveUntranslated();

    void setRemoveUntranslated(boolean removeUntranslated);
  }

  public abstract static class OutputDocumentPostProcessorBase
      implements OutputDocumentPostProcessor {
    boolean removeUntranslated = false;

    public boolean hasRemoveUntranslated() {
      return removeUntranslated;
    }

    @Override
    public void setRemoveUntranslated(boolean removeUntranslated) {
      this.removeUntranslated = removeUntranslated;
    }
  }
}
