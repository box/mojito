package com.box.l10n.mojito.service.drop.exporter;

/**
 * Enum of supported {@link DropExporter} supported
 *
 * TODO(P1) In fact an enum will prevent to add customized exporter in a plugin
 * fashion, probably what not we want to do.
 *
 * @author jaurambault
 */
public enum DropExporterType {

    BOX(BoxDropExporter.class.getName()),
    FILE_SYSTEM(FileSystemDropExporter.class.getName());

    String className;

    DropExporterType(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

}
