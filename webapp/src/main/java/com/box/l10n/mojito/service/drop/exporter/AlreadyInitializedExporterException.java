package com.box.l10n.mojito.service.drop.exporter;

/**
 * Thrown if {@link DropExporter} has already been initialized using {@link DropExporter#init() }
 * and cannot be re-initialized.
 *
 * @author jaurambault
 */
public class AlreadyInitializedExporterException extends RuntimeException {}
