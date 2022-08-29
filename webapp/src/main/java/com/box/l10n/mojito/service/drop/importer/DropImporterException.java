package com.box.l10n.mojito.service.drop.importer;

/**
 * Thrown when a generic error related to a {@link DropImporter} happens
 *
 * @author jaurambault
 */
public class DropImporterException extends Exception {

  public DropImporterException(String message, Throwable cause) {
    super(message, cause);
  }
}
