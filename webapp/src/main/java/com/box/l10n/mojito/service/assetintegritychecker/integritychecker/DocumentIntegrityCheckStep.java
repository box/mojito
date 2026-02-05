package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.RawDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author aloison
 */
@Configurable
public class DocumentIntegrityCheckStep extends BasePipelineStep {

  /** Logger */
  static Logger logger = LoggerFactory.getLogger(DocumentIntegrityCheckStep.class);

  @Autowired IntegrityCheckerFactory integrityCheckerFactory;

  @Override
  public String getName() {
    return "Document Integrity Check";
  }

  @Override
  public String getDescription() {
    return "Runs document-level integrity checks to make sure a document is valid."
        + " When an issue is detected, it will throw an IntegrityCheckException."
        + " Expects: raw document. Sends back: raw document.";
  }

  @Override
  protected Event handleRawDocument(Event event) {
    logger.debug("Check integrity of document");
    RawDocument document = event.getRawDocument();

    String documentContent;
    try {
      Reader reader = document.getReader();
      documentContent = CharStreams.toString(reader);
      // CharStreams#toString does not close the readable implicitly
      reader.close();
    } catch (IOException e) {
      logger.error("Error reading document content", e);
      throw new RuntimeException("Error reading document content", e);
    }

    // TODO(P1): do not hardcode the type here
    List<DocumentIntegrityChecker> documentIntegrityCheckers =
        integrityCheckerFactory.getDocumentCheckers("xliff");
    for (DocumentIntegrityChecker checker : documentIntegrityCheckers) {
      checker.check(documentContent);
    }

    return super.handleRawDocument(event);
  }
}
