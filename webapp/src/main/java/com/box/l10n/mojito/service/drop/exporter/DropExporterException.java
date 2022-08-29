package com.box.l10n.mojito.service.drop.exporter;

/**
 * Thrown when a generic error related to a {@link DropExporter} happens
 *
 * @author jaurambault
 */
public class DropExporterException extends Exception {

  public DropExporterException(String message) {
    super(message);
  }

  public DropExporterException(String message, Throwable cause) {
    super(message, cause);
  }
}
