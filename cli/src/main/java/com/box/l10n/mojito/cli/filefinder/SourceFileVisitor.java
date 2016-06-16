package com.box.l10n.mojito.cli.filefinder;

import com.box.l10n.mojito.cli.filefinder.file.FileType;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File visitor used to find source files.
 * 
 * @author jaurambault
 */
class SourceFileVisitor extends SimpleFileVisitor<Path> {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(SourceFileVisitor.class);

    /**
     * List of source files found
     */
    final List<FileMatch> sourceMatches = new ArrayList<>();

    /**
     * Pattern used to match source file path
     */
    private final FilePattern sourceFilePattern;
    
    /**
     * File type of the source files
     */
    private final FileType fileType;
    
    /**
     * Source directory from which the search is performed
     */
    private final Path sourceDirectory;
    
    /**
     * Regular expression to match source file path in addition to sourceFilePattern
     */
    private final String sourcePathFilterRegex;

    public SourceFileVisitor(FileType fileType, Path sourceDirectory, String sourcePathFilterRegex) {
        this.fileType = fileType;
        this.sourceFilePattern = fileType.getSourceFilePattern();
        this.sourceDirectory = sourceDirectory;
        this.sourcePathFilterRegex = sourcePathFilterRegex;
    }

    /**
     * Gets the list of source files found 
     * 
     * @return the list of source files found 
     */
    public List<FileMatch> getSourceMatches() {
        return sourceMatches;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException ioe) throws IOException {
        if (ioe instanceof AccessDeniedException) {
            logger.error(file + ": cannot access directory");
        } else {
            throw ioe;
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

        Path relativePath = sourceDirectory.relativize(file);

        Matcher matcher = sourceFilePattern.getPattern().matcher(relativePath.toString());

        if (matcher.matches() && matchesSourcePathFilterRegex(relativePath.toString())) {

            FileMatch fileMatch = new FileMatch();
            fileMatch.setFileType(fileType);
            fileMatch.setTarget(false);
            fileMatch.setPath(file);

            for (String group : sourceFilePattern.getGroups()) {
                fileMatch.addProperty(group, matcher.group(group));
            }

            if (!fileMatch.getSourcePath().equals(relativePath.toString())) {
                throw new RuntimeException("this must be equal");
            }

            logger.debug("src name: " + fileMatch.getSourcePath());
            sourceMatches.add(fileMatch);
        }

        return FileVisitResult.CONTINUE;
    }
    
    protected boolean matchesSourcePathFilterRegex(String path) {
        return sourcePathFilterRegex == null || path.matches(sourcePathFilterRegex);
    }
}
