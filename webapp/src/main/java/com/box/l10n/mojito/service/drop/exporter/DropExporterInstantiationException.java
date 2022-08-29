package com.box.l10n.mojito.service.drop.exporter;

/**
 * Thrown if it is not possible to create an instance of a {@link DropExporter} via reflection.
 *
 * @author jaurambault
 */
public class DropExporterInstantiationException extends RuntimeException {

  public DropExporterInstantiationException(String message, Throwable cause) {
    super(message, cause);
  }
}
