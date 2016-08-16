package com.box.l10n.mojito.service.drop.exporter;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Date;

/**
 *
 * @author jaurambault
 */
@JsonPropertyOrder(alphabetic = true)
public class FileSystemDropExporterConfig {

    String dropFolderPath;
    Date uploadDate;

    public String getDropFolderPath() {
        return dropFolderPath;
    }

    public void setDropFolderPath(String dropFolderPath) {
        this.dropFolderPath = dropFolderPath;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(Date uploadDate) {
        this.uploadDate = uploadDate;
    }

}
