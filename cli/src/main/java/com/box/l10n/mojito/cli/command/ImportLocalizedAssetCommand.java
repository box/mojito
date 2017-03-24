package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.LocaleClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.exception.AssetNotFoundException;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.rest.entity.ImportLocalizedAssetBody.StatusForSourceEqTarget;
import com.box.l10n.mojito.rest.entity.Locale;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import com.google.common.collect.HashBiMap;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"import"}, commandDescription = "Import localized assets into the TMS")
public class ImportLocalizedAssetCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ImportLocalizedAssetCommand.class);
    
    @Autowired
    ConsoleWriter consoleWriter;
    
    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;
    
    @Parameter(names = {Param.SOURCE_DIRECTORY_LONG, Param.SOURCE_DIRECTORY_SHORT}, arity = 1, required = false, description = Param.SOURCE_DIRECTORY_DESCRIPTION)
    String sourceDirectoryParam;
    
    @Parameter(names = {Param.TARGET_DIRECTORY_LONG, Param.TARGET_DIRECTORY_SHORT}, arity = 1, required = false, description = Param.TARGET_DIRECTORY_DESCRIPTION)
    String targetDirectoryParam;
    
    @Parameter(names = {"--locale-mapping", "-lm"}, arity = 1, required = false, description = "Locale mapping, format: \"fr:fr-FR,ja:ja-JP\". "
            + "The keys contain BCP47 tags of the generated files and the values indicate which repository locales are used to fetch the translations.")
    String localeMappingParam;
    
    @Parameter(names = {Param.FILE_TYPE_LONG, Param.FILE_TYPE_SHORT}, arity = 1, required = false, description = Param.FILE_TYPE_DESCRIPTION,
            converter = FileTypeConverter.class)
    FileType fileType;
    
    @Parameter(names = {Param.SOURCE_LOCALE_LONG, Param.SOURCE_LOCALE_SHORT}, arity = 1, required = false, description = Param.SOURCE_LOCALE_DESCRIPTION)
    String sourceLocale;
    
    @Parameter(names = {Param.SOURCE_REGEX_LONG, Param.SOURCE_REGEX_SHORT}, arity = 1, required = false, description = Param.SOURCE_REGEX_DESCRIPTION)
    String sourcePathFilterRegex;
    
    @Parameter(names = {"--source-equals-target"}, required = false, description = "Status of the imported translation when the target is the same as the source (SKIPPED means no import)",
            converter = ImportLocalizedAssetBodyStatusForSourceEqTarget.class)
    StatusForSourceEqTarget statusForSourceEqTarget = null;
    
    @Autowired
    AssetClient assetClient;
    
    @Autowired
    LocaleClient localeClient;
    
    @Autowired
    RepositoryClient repositoryClient;
    
    @Autowired
    CommandHelper commandHelper;
    
    Repository repository;
    
    CommandDirectories commandDirectories;

    /**
     * Contains a map of locale for generating localized file a locales defined
     * in the repository.
     */
    Map<String, String> inverseLocaleMapping;
    
    @Override
    public void execute() throws CommandException {
        
        consoleWriter.newLine().a("Start importing localized files for repository: ").fg(Ansi.Color.CYAN).a(repositoryParam).println(2);
        
        repository = commandHelper.findRepositoryByName(repositoryParam);
        commandDirectories = new CommandDirectories(sourceDirectoryParam, targetDirectoryParam);
        inverseLocaleMapping = commandHelper.getInverseLocaleMapping(localeMappingParam);  
        
        
        for (FileMatch sourceFileMatch : commandHelper.getSourceFileMatches(commandDirectories, fileType, sourceLocale, sourcePathFilterRegex)) {
            for (Locale locale : getSortedRepositoryLocales()) {
                doImportFileMatch(sourceFileMatch, locale);
            }
        }
        
        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }
    
    protected void doImportFileMatch(FileMatch fileMatch, Locale locale) throws CommandException {
        try {
            logger.info("Importing for locale: {}", locale.getBcp47Tag());
            Path targetPath = getTargetPath(fileMatch, locale);
            
            consoleWriter.a(" - Importing file: ").fg(Ansi.Color.MAGENTA).a(targetPath.toString()).fg(Ansi.Color.YELLOW).a(" Running").println();
            
            Asset assetByPathAndRepositoryId = assetClient.getAssetByPathAndRepositoryId(fileMatch.getSourcePath(), repository.getId());
            
            assetClient.importLocalizedAssetForContent(assetByPathAndRepositoryId.getId(), locale.getId(), commandHelper.getFileContent(targetPath), statusForSourceEqTarget);
            
            consoleWriter.erasePreviouslyPrintedLines();
            consoleWriter.a(" - Importing file: ").fg(Ansi.Color.MAGENTA).a(targetPath.toString()).fg(Ansi.Color.GREEN).a(" Done").println();
            
        } catch (AssetNotFoundException ex) {
            throw new CommandException("No asset for file [" + fileMatch.getPath() + "] into repo [" + repositoryParam + "]", ex);
        }
    }
    
    protected List<Locale> getSortedRepositoryLocales() {
        List<Locale> locales = new ArrayList<>();
        
        ArrayDeque<RepositoryLocale> toProcess = new ArrayDeque<>(repository.getRepositoryLocales());
        Locale rootLocale = null;
        
        for (RepositoryLocale rl : toProcess) {
            if (rl.getParentLocale() == null) {
                rootLocale = rl.getLocale();
                toProcess.remove(rl);
                break;
            }
        }
        
        Set<Long> localeIds = new HashSet<>();
        
        while (!toProcess.isEmpty()) {
            RepositoryLocale rl = toProcess.removeFirst();
            Long parentLocaleId = rl.getParentLocale().getLocale().getId();
            if (parentLocaleId.equals(rootLocale.getId()) || localeIds.contains(parentLocaleId)) {
                localeIds.add(rl.getLocale().getId());
                locales.add(rl.getLocale());
            } else {
                toProcess.addLast(rl);
            }
        }
        
        return locales;
    }
     
    
    private Path getTargetPath(FileMatch fileMatch, Locale locale) throws CommandException {
        
        String targetLocale;
        
        if (inverseLocaleMapping != null) {          
            targetLocale = inverseLocaleMapping.get(locale.getBcp47Tag());
        } else {
            targetLocale = locale.getBcp47Tag();
        }
        
        logger.info("processing locale for import: {}", targetLocale);
        Path targetPath = commandDirectories.getTargetDirectoryPath().resolve(fileMatch.getTargetPath(targetLocale));
        
        return targetPath;
    }
    
}
