package com.box.l10n.mojito.service.drop;

import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TranslationKit;
import com.box.l10n.mojito.service.drop.exporter.DropExporter;
import com.box.l10n.mojito.service.drop.exporter.DropExporterException;
import com.box.l10n.mojito.service.drop.exporter.DropExporterService;
import com.box.l10n.mojito.service.drop.importer.DropFile;
import com.box.l10n.mojito.service.drop.importer.DropImporter;
import com.box.l10n.mojito.service.drop.importer.DropImporterException;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.InjectCurrentTask;
import com.box.l10n.mojito.service.pollableTask.MsgArg;
import com.box.l10n.mojito.service.pollableTask.ParentTask;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.UpdateTMWithXLIFFResult;
import com.box.l10n.mojito.service.translationkit.TranslationKitAsXliff;
import com.box.l10n.mojito.service.translationkit.TranslationKitService;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.exceptions.OkapiIOException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to generate {@link Drop}s.
 *
 * @author jaurambault
 */
@Service
public class DropService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DropService.class);

    @Autowired
    DropRepository dropRepository;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    DropExporterService dropExporterService;

    @Autowired
    LocaleService localeService;

    @Autowired
    TranslationKitService translationKitService;

    @Autowired
    TMService tmService;

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    DropServiceConfig dropServiceConfig;

    /**
     * Creates a new {@link Drop} for a {@link Repository}.
     *
     * @param repository
     * @return created {@link Drop}
     */
    public Drop createDrop(Repository repository) {

        logger.debug("Create drop for repository id: {}", repository.getId());

        Drop drop = new Drop();
        drop.setRepository(repository);
        drop.setDropExporterType(repository.getDropExporterType());
        drop.setName(getDropName(new Date()));

        return dropRepository.save(drop);
    }

    /**
     * Starts {@link Drop} export process by creating a {@link PollableTask},
     * the drop entity and then kick off the actual export process as an async
     * task.
     *
     * @param exportDropConfig contains the configuration to create the drop
     * @param currentTask
     * @return A {@link PollableFuture} for tracking
     * @throws DropExporterException
     */
    @Pollable(expectedSubTaskNumber = 1, message = "Start the Drop export process")
    public PollableFuture<Drop> startDropExportProcess(
            ExportDropConfig exportDropConfig,
            @InjectCurrentTask PollableTask currentTask) throws DropExporterException {

        Repository repository = repositoryRepository.findOne(exportDropConfig.getRepositoryId());
        Drop drop = createDrop(repository);
        drop.setExportPollableTask(currentTask);

        createDropExporterAndExportTranslationKits(drop, exportDropConfig.getBcp47Tags(), exportDropConfig.getType(), exportDropConfig.getUseInheritance(), currentTask, PollableTask.INJECT_CURRENT_TASK);

        return new PollableFutureTaskResult<>(drop);
    }

    /**
     * Creates a {@link DropExporter} and exports {@link TranslationKit}s for
     * given {@link Drop} and list of locales.
     *
     * @param drop {@link Drop}
     * @param bcp47Tags list of bcp47 tags of the {@link TranslationKit}s
     * @param type type of the {@link TranslationKit}s
     * @param useInheritance use inherited translations from parent locales when creating {@link TranslationKit}
     * @param parentTask
     */
    @Pollable(async = true)
    private PollableFuture<DropExporter> createDropExporterAndExportTranslationKits(
            Drop drop,
            List<String> bcp47Tags,
            TranslationKit.Type type,
            Boolean useInheritance,
            @ParentTask PollableTask parentTask,
            @InjectCurrentTask PollableTask currentTask) throws DropExporterException {

        PollableFutureTaskResult<DropExporter> pollableFutureTaskResult = new PollableFutureTaskResult<>();

        try {
            logger.debug("Update the expected sub-tasks number: 1 task to prepare the drop and as many tasks as there are languages");
            pollableFutureTaskResult.setExpectedSubTaskNumberOverride(bcp47Tags.size() + 1);

            DropExporter dropExporter = dropExporterService.createDropExporterAndUpdateDrop(drop, currentTask);
            pollableFutureTaskResult.setResult(dropExporter);

            for (String bcp47Tag : bcp47Tags) {
                generateAndExportTranslationKit(bcp47Tag, type, useInheritance, drop, dropExporter, currentTask);
            }
        } catch (Throwable t) {
            drop.setExportFailed(Boolean.TRUE);
            dropRepository.save(drop);
            throw t;
        }

        return pollableFutureTaskResult;
    }

    /**
     * Generates the {@link TranslationKit}s for each locales and then export
     * them using provided {@link DropExporter}.
     *
     * @param bcp47Tag list of bcp47 tags of the {@link TranslationKit}s
     * @param type type of the {@link TranslationKit}s
     * @param useInheritance use inherited translations from parent locales when creating {@link TranslationKit}
     * @param drop the {@link Drop} related to the {@link TranslationKit}s
     * @param dropExporter used to export the {@link TranslationKit}s
     * @param parentTask
     * @throws DropExporterException
     */
    @Pollable(message = "Generate and export translation kit for locale: {bcp47Tag}")
    private void generateAndExportTranslationKit(
            @MsgArg(name = "bcp47Tag") String bcp47Tag,
            TranslationKit.Type type,
            Boolean useInheritance,
            Drop drop,
            DropExporter dropExporter,
            @ParentTask PollableTask parentTask) throws DropExporterException {

        logger.trace("Generate and export translation kits for locale: {}", bcp47Tag);

        TranslationKitAsXliff translationKitAsXLIFF = translationKitService.generateTranslationKitAsXLIFF(
                drop.getId(),
                drop.getRepository().getTm().getId(),
                localeService.findByBcp47Tag(bcp47Tag).getId(),
                type,
                useInheritance);

        if (!translationKitAsXLIFF.isEmpty()) {
            dropExporter.exportSourceFile(bcp47Tag, translationKitAsXLIFF.getContent());
        }
    }

    /**
     * Imports a {@link Drop} from a {@link DropImporter} into the {@link TM}.
     * <p/>
     * <p/>
     * Underlying implementation assume files are in XLIFF format
     *
     * @param dropId {@link Drop#id} of the drop to be imported
     * @param importStatus specific status to use when importing translation
     * @param currentTask
     * @return
     * @throws DropImporterException
     * @throws DropExporterException
     * @throws ImportDropException
     */
    @Pollable(async = true, message = "Start importing drop", expectedSubTaskNumber = 1)
    public PollableFuture importDrop(
            Long dropId,
            TMTextUnitVariant.Status importStatus,
            @InjectCurrentTask PollableTask currentTask) throws DropImporterException, DropExporterException, ImportDropException {

        PollableFutureTaskResult pollableFutureTaskResult = new PollableFutureTaskResult();

        logger.debug("Start importing drop");

        Drop drop = dropRepository.findOne(dropId);
        drop.setLastImportedDate(new DateTime());
        drop.setImportPollableTask(currentTask);
        drop.setImportFailed(null);
        dropRepository.save(drop);

        try {
            DropExporter dropExporter = dropExporterService.recreateDropExporter(drop);
            DropImporter dropImporter = dropExporter.getDropImporter();

            List<DropFile> files = dropImporter.getFiles();

            logger.debug("Update the expected sub-tasks number: as many tasks as there are localized files");
            pollableFutureTaskResult.setExpectedSubTaskNumberOverride(files.size());

            int numberOfFailedImport = 0;

            for (DropFile file : files) {
                try {
                    importFile(dropImporter, dropExporter, file, importStatus, currentTask, PollableTask.INJECT_CURRENT_TASK);
                } catch (Throwable t) {
                    logger.debug("Error when importing file, keep importing other files", t);
                    ++numberOfFailedImport;
                    markLocalizedFileAsInvalid(dropExporter, file, t);
                }
            }

            if (numberOfFailedImport > 0) {
                throw new ImportDropException("Number of files not imported: " + numberOfFailedImport + ", check sub task for more information");
            }
        } catch (Throwable t) {
            drop.setImportFailed(Boolean.TRUE);
            dropRepository.save(drop);
            throw t;
        }

        return pollableFutureTaskResult;
    }

    /**
     * Imports a {@link DropFile} into the {@link TM} and then export the
     * imported file that has been enriched with meta information during the
     * import process.
     * <p/>
     * <p/>
     * Underlying implementation assumes the file is in XLIFF format
     *
     * @param dropImporter
     * @param dropFile
     * @param importStatus specific status to use when importing translation
     * @param parentTask
     * @param currentTask
     * @throws Exception
     */
    @Pollable(expectedSubTaskNumber = 3, message = "Importing file: {fileName}")
    private void importFile(
            DropImporter dropImporter,
            DropExporter dropExporter,
            @MsgArg(name = "fileName", accessor = "getName") DropFile dropFile,
            TMTextUnitVariant.Status importStatus,
            @ParentTask PollableTask parentTask,
            @InjectCurrentTask PollableTask currentTask) throws DropImporterException, DropExporterException, ImportDropException {

        logger.debug("Import file: {}", dropFile.getName());
        downloadDropFileContent(dropImporter, dropFile, currentTask);
        UpdateTMWithXLIFFResult updateReport = updateTMWithLocalizedXLIFF(dropFile, importStatus, currentTask);
        exportImportedFile(dropExporter, dropFile, updateReport.getXliffContent(), updateReport.getComment(), currentTask);
    }

    /**
     * Simple wrapper on
     * {@link TMService#updateTMWithLocalizedXLIFF(String, Locale, boolean, TMTextUnitVariant.Status)} to
     * support {@link Pollable}
     *
     * @param dropFile
     * @param importStatus specific status to use when importing translation
     * @param parentTask
     */
    @Pollable(message = "Update TM with file: {fileName}")
    private UpdateTMWithXLIFFResult updateTMWithLocalizedXLIFF(
            @MsgArg(name = "fileName", accessor = "getName") DropFile dropFile,
            TMTextUnitVariant.Status importStatus,
            @ParentTask PollableTask parentTask) throws ImportDropException {

        try {
            return tmService.updateTMWithTranslationKitXLIFF(dropFile.getContent(), importStatus, dropServiceConfig.getDropImporterUsername());
        } catch (OkapiBadFilterInputException | OkapiIOException okapiException) {
            throw new ImportDropException(okapiException.getMessage(), okapiException);
        }
    }

    @Pollable(message = "Export the \"imported\" file with meta information related to import process: {fileName}")
    private void exportImportedFile(
            DropExporter dropExporter,
            @MsgArg(name = "fileName", accessor = "getName") DropFile dropFile,
            String importedFileContent,
            String importedFileComment,
            @ParentTask PollableTask parentTask) throws DropExporterException {

        dropExporter.exportImportedFile(dropFile.getName(), importedFileContent, importedFileComment);
    }

    /**
     * Downloads the {@link DropFile#content} using a {@link DropImporter}.
     *
     * @param dropImporter used to fetch the content
     * @param dropFile drop file for which content should be fetch
     * @param parentTask
     * @throws DropImporterException
     */
    @Pollable(message = "Fetch DropFile content (filename: {fileName})")
    private void downloadDropFileContent(
            DropImporter dropImporter,
            @MsgArg(name = "fileName", accessor = "getName") DropFile dropFile,
            @ParentTask PollableTask parentTask) throws DropImporterException {

        dropImporter.downloadFileContent(dropFile);
    }

    /**
     * Marks a file as invalid by adding a simple comment to it.
     *
     * @param dropExporter the exporter use to actually add the comment on the
     * file
     * @param file the file to be marked as invalid
     * @param cause contains the cause of file invalidity
     */
    private void markLocalizedFileAsInvalid(DropExporter dropExporter, DropFile file, Throwable cause) {
        try {
            dropExporter.addCommentToFile(file.getId(), "Invalid localized file must be reviewed.\n" + cause.getMessage());
        } catch (Throwable t) {
            logger.debug("Cannot add mark source file as invalid", t);
        }
    }

    /**
     * Gets the drop folder name.
     *
     * @param uploadTime time of the upload
     * @return the folder name
     */
    String getDropName(Date uploadTime) {

        Calendar uploadCalendar = Calendar.getInstance();
        uploadCalendar.setTime(uploadTime);
        
        Calendar currentWeek = Calendar.getInstance();
        currentWeek.setTime(uploadTime);
        currentWeek.set(Calendar.WEEK_OF_YEAR, currentWeek.get(Calendar.WEEK_OF_YEAR) - dropServiceConfig.getDropNameWeekOffset());

        return "Week " + currentWeek.get(Calendar.WEEK_OF_YEAR) + new SimpleDateFormat(" (EEEE) - dd MMMM YYYY - HH.mm.ss").format(uploadTime);
    }


    /**
     * Cancels a {@link Drop}.  A {@link Drop} can only be canceled if it's not in the middle of being exported.
     *
     * @param dropId {@link Drop#id} of the drop to be imported
     * @param currentTask Current task
     * @return {@link PollableFuture}
     */
    @Pollable(async = true, message = "Start canceling drop")
    public PollableFuture<Drop> cancelDrop(Long dropId, @InjectCurrentTask PollableTask currentTask) throws DropExporterException, CancelDropException {

        logger.debug("Canceling Drop: {}", dropId);
        Drop drop = dropRepository.findOne(dropId);

        if (isDropBeingProcessed(drop)) {
            throw new CancelDropException("A Drop [" + dropId + "] cannot be canceled while it is not at rest");
        }

        drop.setCanceled(true);
        dropRepository.save(drop);

        PollableFutureTaskResult<Drop> pollableFutureTaskResult = new PollableFutureTaskResult<>();

        DropExporter dropExporter = dropExporterService.recreateDropExporter(drop);

        logger.debug("Deleting Drop using exporter");
        dropExporter.deleteDrop();

        pollableFutureTaskResult.setResult(drop);

        return pollableFutureTaskResult;
    }

    /**
     * Checks to see if {@link Drop} is being process
     *
     * @param drop {@link Drop} to check
     * @return True if it is
     */
    protected boolean isDropBeingProcessed(Drop drop) {
        return !hasDropExportStart(drop) ||
                (drop.getExportPollableTask() != null && !drop.getExportPollableTask().isAllFinished()) ||
                (drop.getImportPollableTask() != null && !drop.getImportPollableTask().isAllFinished());
    }

    /**
     * Check if the export has started
     * @param drop
     * @return
     */
    protected boolean hasDropExportStart(Drop drop) {
        return drop.getDropExporterConfig() != null;
    }

    /**
     * Force complete a partially imported drop
     *
     * @param drop
     */
    @Transactional
    public void completeDrop(Drop drop) {
        drop.setPartiallyImported(Boolean.FALSE);
        dropRepository.save(drop);
    }
}
