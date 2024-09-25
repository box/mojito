package com.box.l10n.mojito.okapi.steps;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;

/**
 * This is a copy of Okapi's FilterEventsToRawDocumentStep with minor modifications to support
 * streams instead of file for the output.
 *
 * @author jaurambault
 */
@UsingParameters() // No parameters
public class FilterEventsToInMemoryRawDocumentStep extends BasePipelineStep {

  private IFilterWriter filterWriter;
  private File outputFile;
  private URI outputURI;
  private LocaleId targetLocale;
  private String outputEncoding;
  private RawDocument rawDocument;
  ByteArrayOutputStream outputDocument;

  /** Create a new FilterEventsToRawDocumentStep object. */
  public FilterEventsToInMemoryRawDocumentStep() {}

  @StepParameterMapping(parameterType = StepParameterType.OUTPUT_URI)
  public void setOutputURI(URI outputURI) {
    this.outputURI = outputURI;
  }

  @SuppressWarnings("deprecation")
  @StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
  public void setTargetLocale(LocaleId targetLocale) {
    this.targetLocale = targetLocale;
  }

  @StepParameterMapping(parameterType = StepParameterType.OUTPUT_ENCODING)
  public void setOutputEncoding(String outputEncoding) {
    this.outputEncoding = outputEncoding;
  }

  @StepParameterMapping(parameterType = StepParameterType.INPUT_RAWDOC)
  public void setInputDocument(RawDocument rawDocument) {
    this.rawDocument = rawDocument;
  }

  @Override
  public String getName() {
    return "Filter Events to In Memory Raw Document";
  }

  @Override
  public String getDescription() {
    return "Combine filter events into a full in memory document and pass it along as a raw document."
        + " Expects: filter events. Sends back: in memory raw document.";
  }

  /**
   * Catch all incoming {@link Event}s and write them out to the output document. This step
   * generates NO_OP events until the input events are exhausted, at which point a RawDocument event
   * is sent.
   */
  @Override
  public Event handleEvent(Event event) {
    switch (event.getEventType()) {
      case START_DOCUMENT:
        handleStartDocument(event);
        return Event.createNoopEvent();

      case END_DOCUMENT:
        return processEndDocument(event);

      case START_SUBDOCUMENT:
      case START_GROUP:
      case END_SUBDOCUMENT:
      case END_GROUP:
      case START_SUBFILTER:
      case END_SUBFILTER:
      case DOCUMENT_PART:
      case TEXT_UNIT:
        // handle all the events between START_DOCUMENT and END_DOCUMENT
        filterWriter.handleEvent(event);
        return Event.createNoopEvent();
    }

    // Else, just return the event
    return event;
  }

  private Event processEndDocument(Event event) {
    // Handle the END_DOCUMENT event and close the writer
    filterWriter.handleEvent(event);
    filterWriter.close();
    try {
      // Return the RawDocument Event that is the end result of all previous Events
      // Note that the source locale is now set to the 'target locale' value since it is an output
      // We also set the target to the same value to have a value
      inMemoryOutputDocuments.put(rawDocument, getOutputDocument());
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }

    RawDocument input =
        new RawDocument(outputFile.toURI(), outputEncoding, targetLocale, targetLocale);
    return new Event(EventType.RAW_DOCUMENT, input);
  }

  String getOutputDocument() throws UnsupportedEncodingException {
    String outputDocument = this.outputDocument.toString(outputEncoding);

    OutputDocumentPostProcessingAnnotation outputDocumentPostProcessingAnnotation =
        rawDocument.getAnnotation(OutputDocumentPostProcessingAnnotation.class);
    if (outputDocumentPostProcessingAnnotation != null
        && outputDocumentPostProcessingAnnotation.getOutputDocumentPostProcessor() != null) {
      outputDocument =
          outputDocumentPostProcessingAnnotation
              .getOutputDocumentPostProcessor()
              .execute(outputDocument);
    }

    return outputDocument;
  }

  @Override
  protected Event handleStartDocument(Event event) {

    StartDocument startDoc = (StartDocument) event.getResource();
    if (outputEncoding == null) {
      outputEncoding = startDoc.getEncoding();
    }

    filterWriter = startDoc.getFilterWriter();
    filterWriter.setOptions(targetLocale, outputEncoding);

    // start customized code
    if (outputFile == null) {
      // fake file for okapi
      outputFile = new File(UUID.randomUUID().toString());
    }

    outputDocument = new ByteArrayOutputStream();
    filterWriter.setOutput(outputDocument);
    // end customized code

    filterWriter.handleEvent(event);
    return event;
  }

  protected Map<RawDocument, String> inMemoryOutputDocuments = new HashMap<>();

  public String getOutput(RawDocument rawDocument) {
    return inMemoryOutputDocuments.get(rawDocument);
  }
}
