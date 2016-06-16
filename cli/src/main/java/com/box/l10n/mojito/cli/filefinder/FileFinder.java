package com.box.l10n.mojito.cli.filefinder;

import com.box.l10n.mojito.cli.filefinder.file.AndroidStringsFileType;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.cli.filefinder.file.MacStringsFileType;
import com.box.l10n.mojito.cli.filefinder.file.PropertiesFileType;
import com.box.l10n.mojito.cli.filefinder.file.ReswFileType;
import com.box.l10n.mojito.cli.filefinder.file.ResxFileType;
import com.box.l10n.mojito.cli.filefinder.file.XliffFileType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code FileFinder} class is used to search source and target files for
 * different types: Xliff, properties, IOS, Android, etc.
 *
 * Source files are files that contains content to be translated and target file
 * are localized files.
 *
 * @author jaurambault
 */
public class FileFinder {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(FileFinder.class);

    /**
     * Directory where to search for source files
     */
    private Path sourceDirectory;

    /**
     * Directory where to search for target files
     */
    private Path targetDirectory;

    /**
     * Types of file to search for
     */
    private List<FileType> fileTypes = new ArrayList<>();

    /**
     * Regular expression to filter source paths
     */
    private String sourcePathFilterRegex;

    /**
     * The result of the search
     */
    FileFinderResult fileFinderResult;

    /**
     * Creates an instance with default file types registered.
     *
     * To set specific types, use {@link #setFileTypes(com.box.l10n.mojito.cli.filefinder.file.FileType...)
     * }
     *
     */
    public FileFinder() {
        fileTypes.add(new XliffFileType());
        fileTypes.add(new PropertiesFileType());
        fileTypes.add(new AndroidStringsFileType());
        fileTypes.add(new MacStringsFileType());
        fileTypes.add(new ReswFileType());
        fileTypes.add(new ResxFileType());
    }

    public Path getSourceDirectory() {
        return sourceDirectory;
    }

    public void setSourceDirectory(Path sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public Path getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(Path targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public String getSourcePathFilterRegex() {
        return sourcePathFilterRegex;
    }

    public void setSourcePathFilterRegex(String sourcePathFilterRegex) {
        this.sourcePathFilterRegex = sourcePathFilterRegex;
    }

    /**
     * Sets the types of file to search for
     *
     * @param fileTypes types of file to search for
     */
    public void setFileTypes(FileType... fileTypes) {
        this.fileTypes = Arrays.asList(fileTypes);
    }

    /**
     * Gets the source files matches.
     *
     * @return the source files matches.
     */
    public ArrayList<FileMatch> getSources() {

        if (fileFinderResult == null) {
            throw new RuntimeException("find() must be called first");
        }

        return fileFinderResult.getSources();
    }

    /**
     * Gets the target files matches.
     *
     * @return the target files matches.
     */
    public ArrayList<FileMatch> getTargets() {

        if (fileFinderResult == null) {
            throw new RuntimeException("find() must be called first");
        }

        return fileFinderResult.getTargets();
    }

    /**
     * Search for source and target files for all registered type in the finder.
     *
     * Result can be retrieved via {@link #getSources() } and {@link #getTargets()
     * }
     *
     * @throws FileFinderException
     */
    public void find() throws FileFinderException {

        fileFinderResult = new FileFinderResult();

        for (FileType fileType : fileTypes) {
            logger.debug("Find matches for type: {}", fileType.getClass().getName());
            FileFinderResult fileFinderResultByType = find(fileType);

            logger.debug("Merge find for type into all matches");
            fileFinderResult.getSources().addAll(fileFinderResultByType.getSources());
            fileFinderResult.getTargets().addAll(fileFinderResultByType.getTargets());
        }
    }

    /**
     * Finds files for a given type.
     *
     * @param fileType the file type to search file for
     * @return source and target files found
     *
     * @throws FileFinderException
     */
    private FileFinderResult find(FileType fileType) throws FileFinderException {

        FileFinderResult fileFinderResultByType = new FileFinderResult();

        List<FileMatch> sourceMatchCandidates = getSourceMatcheCandidates(fileType);
        List<FileMatch> targetMatcheCandidates = getTargetMatcheCandidates(fileType);

        targetMatcheCandidates = filterTargetMatchesWithoutSourceMatches(sourceMatchCandidates, targetMatcheCandidates);
        sourceMatchCandidates = filterSourceMatchesThatAreTargetMatches(sourceMatchCandidates, targetMatcheCandidates);

        fileFinderResultByType.getSources().addAll(sourceMatchCandidates);
        fileFinderResultByType.getTargets().addAll(targetMatcheCandidates);

        return fileFinderResultByType;
    }

    private List<FileMatch> getSourceMatcheCandidates(FileType fileType) throws FileFinderException {

        if (!sourceDirectory.toFile().isDirectory()) {
            throw new FileFinderException("Invalid source directory: " + sourceDirectory.toString());
        }

        SourceFileVisitor sourceFileVisitor = new SourceFileVisitor(fileType, sourceDirectory, sourcePathFilterRegex);

        try {
            Files.walkFileTree(sourceDirectory, sourceFileVisitor);
        } catch (IOException ex) {
            throw new FileFinderException("Error while looking for source files", ex);
        }

        return sourceFileVisitor.getSourceMatches();
    }

    private List<FileMatch> getTargetMatcheCandidates(FileType fileType) throws FileFinderException {

        if (!targetDirectory.toFile().isDirectory()) {
            throw new FileFinderException("Invalid target directory: " + targetDirectory.toString());
        }

        TargetFileVisitor targetFileVisitor = new TargetFileVisitor(fileType, targetDirectory);

        try {
            Files.walkFileTree(targetDirectory, targetFileVisitor);
        } catch (IOException ex) {
            throw new FileFinderException("Error while looking for target files", ex);
        }

        return targetFileVisitor.getTargetMatches();
    }

    /**
     * Filter the list of target matches to return only the ones that have
     * matching source.
     *
     * @param sourceMatchCandidates list of source matches
     * @param targetMatchCandidates list of target match candidates
     * @return the filtered target matches
     *
     */
    private List<FileMatch> filterTargetMatchesWithoutSourceMatches(List<FileMatch> sourceMatchCandidates, List<FileMatch> targetMatchCandidates) {

        List<FileMatch> filteredTargetMatchCandidates = new ArrayList<>();

        List<String> sourceFilenames = getSourceFilenames(sourceMatchCandidates);

        for (FileMatch targetMatchCandidate : targetMatchCandidates) {
            if (sourceFilenames.contains(targetMatchCandidate.getSourcePath())) {
                filteredTargetMatchCandidates.add(targetMatchCandidate);
            }
        }

        return filteredTargetMatchCandidates;
    }

    /**
     * Removes from source matches entries that are also a target match.
     *
     * In some cases regex can match both as a source and a target (eg.
     * file.fr.resx will be returned as source with basename: file.fr and target
     * with basename: file) so in that case the file is consider to be a target
     * file and not a source file.
     *
     * @param sourceMatchCandidates list of source match to be filtered
     * @param targetMatchCandidates list of target matches
     * @return the filtered source matches
     */
    private List<FileMatch> filterSourceMatchesThatAreTargetMatches(List<FileMatch> sourceMatchCandidates, List<FileMatch> targetMatchCandidates) {

        List<FileMatch> filteredSourceMatchCandidates = new ArrayList<>();

        List<Path> targetPaths = getPaths(targetMatchCandidates);

        for (FileMatch sourceMatchCandidate : sourceMatchCandidates) {
            if (!targetPaths.contains(sourceMatchCandidate.getPath())) {
                filteredSourceMatchCandidates.add(sourceMatchCandidate);
            }
        }

        return filteredSourceMatchCandidates;
    }

    /**
     * Gets source filenames form a list of {@link FileMatch}s
     *
     * @param fileMatches
     * @return source filenames
     */
    private List<String> getSourceFilenames(List<FileMatch> fileMatches) {

        List<String> sourceFilenames = new ArrayList<>();

        for (FileMatch fileMatch : fileMatches) {
            sourceFilenames.add(fileMatch.getSourcePath());
        }

        return sourceFilenames;
    }

    /**
     * Gets list of {@link Path}s for a list of {@link FileMatch}s.
     *
     * @param fileMatches
     * @return
     */
    private List<Path> getPaths(List<FileMatch> fileMatches) {
        List<Path> paths = new ArrayList<>();

        for (FileMatch fileMatch : fileMatches) {
            paths.add(fileMatch.getPath());
        }

        return paths;
    }

}
