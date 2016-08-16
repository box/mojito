package com.box.l10n.mojito.service.drop.exporter;

import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.service.drop.importer.DropImporter;

/**
 * Implementing this interface allows an object to be used to export
 * {@link Drop}s.
 *
 * <p>
 * DropExporter instance should be re-creatable from a configuration string. See
 * {@link DropExporterService#createDropExporterAndUpdateDrop(com.box.l10n.mojito.entity.Drop, com.box.l10n.mojito.entity.PollableTask) }
 * and
 * {@link DropExporterService#recreateDropExporter(com.box.l10n.mojito.entity.Drop) }
 *
 * <h3>Create initial instance followed by creating an instance from config</h3>
 * <pre>
 DropExporter e = ...;
 e.init();
 String eConfig = e.getConfig();

 DropExporter e2 = ...;
 e2.setConfig(eConfig);
 e2.init();

 assertEquals(e.getConfig(), e2.getConfig());

 e.exportSourceFile("fr-FR", fileContent);
 // must be equivalent to
 e2.exportSourceFile("fr-FR", fileContent);
 </pre>
 *
 * @author jaurambault
 */
public interface DropExporter {

    /**
     * @return the {@link DropExporterType} implemented by this instance.
     */
    DropExporterType getDropExporterType();

    /**
     * Gets a string that contains the instance configuration.
     *
     * <p>
     * The configuration must contain enough information to re-create a fully
     * workable instance, using {@link #setConfig(java.lang.String) }
     * followed by {@link #init(com.box.l10n.mojito.entity.PollableTask, com.box.l10n.mojito.entity.PollableTask)
     * }.
     *
     * <p>
     * The configuration can any semi-structured data and is up to the
     * implementation
     *
     * @return the exporter configuration as a String
     */
    String getConfig();

    /**
     * Sets the filter configuration to initialize the instance.
     *
     * @param config the configuration to be used to initialized
     * ({@link #init() the instance
     * @throws AlreadyInitializedExporterException if
     * the filter has already been initialized and the config cannot be changed.
     * @throws DropExporterException if an error occurred when exporting the file
     */
    void setConfig(String config) throws DropExporterException, AlreadyInitializedExporterException;

    /**
     * Initialize the DropExporter (for example, an implementation could prepare
     * the target location by creating directories where the drop files will be
     * store).
     *
     * <p>
     * Optionally use {@link #getConfig() } to initialize the instance.
     *
     * <p>
     * Once the instance is initialized, {@link #getConfig()} must return enough
     * information to fully re-create a new instance of the exporter equivalent
     * to the current instance.
     *
     * @param dropGroupName group name associated with this exporter
     * @param dropName name of the drop
     * @throws DropExporterException if an error occurred when exporting the file
     */
    void init(String dropGroupName, String dropName) throws DropExporterException;

    /**
     * Exports a source file via the {@link DropExporter}. The file content is assumed
     * to be XLIFF.
     *
     * @param bcp47tag BCP47 tag
     * @param fileContent file content in XLIFF unicode-normalized in NFC
     * @throws DropExporterException if an error occurred when exporting the file
     */
    void exportSourceFile(String bcp47tag, String fileContent) throws DropExporterException;

    /**
     * Exports an "imported" file (a localized file that was imported and that
     * contains meta information about the import process). This file can be
     * used to review what text unit were saved or skipped.
     *
     * @param filename filename
     * @param fileContent file content in XLIFF unicode-normalized in NFC
     * @param comment a comment related to the imported file
     * @throws DropExporterException
     */
    void exportImportedFile(String filename, String fileContent, String comment) throws DropExporterException;
    
    /**
     * Adds a comment to a file 
     * 
     * @param fileId the file id
     * @param comment the comment to be added
     * @throws DropExporterException 
     */
    void addCommentToFile(String fileId, String comment) throws DropExporterException;

    /**
     * Returns an {@link DropImporter} to read localized files corresponding to
     * source files exported using this exporter (or a re-created exporter).
     *
     * @return
     */
    DropImporter getDropImporter();

    /**
     * Deletes all resources that were created while exporting the {@link Drop}
     *
     * @throws DropExporterException if an error occurred when exporting the file
     */
    void deleteDrop() throws DropExporterException;
}
