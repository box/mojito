package com.box.l10n.mojito.service.drop.exporter;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the {@link FileSystemDropExporter}.
 * 
 * @author jaurambault
 */
@Component
@ConfigurationProperties("l10n.filesystemdropexporter")
public class FileSystemDropExporterConfigFromProperties {

    /**
     * Path on the file system where drops are exported, if not provided the
     * exporter will use the "fileSystemDropExporter" subdirectory in the
     * current temporary directory.
     */
    Path basePath = Paths.get(System.getProperty("java.io.tmpdir"), "fileSystemDropExporter");

    public Path getBasePath() {
        return basePath;
    }

    public void setBasePath(Path basePath) {
        this.basePath = basePath;
    }

}
