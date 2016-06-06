package com.box.l10n.mojito.service.drop.importer;

import java.util.List;

/**
 * Implementing this interface allows an object to retrieve content generated
 * by {@link DropExporter}.
 *
 * <p>
 * The {@link DropImporter} related to a specific {@link DropExporter} can be
 * retrieved with {@link DropExporter#getDropImporter() }
 *
 * <p>All files are expected to be in XLIFF format for import.
 *
 * @author jaurambault
 */
public interface DropImporter {

    /**
     * Download the file content (in XLIFF format).
     *
     * @param dropFile drop file for which the content should be downloaded
     * @throws DropImporterException
     */
    public void downloadFileContent(DropFile dropFile) throws DropImporterException;

    /**
     * Gets the list of files that can be imported.
     *
     * @return
     * @throws DropImporterException
     */
    public List<DropFile> getFiles() throws DropImporterException;

}
