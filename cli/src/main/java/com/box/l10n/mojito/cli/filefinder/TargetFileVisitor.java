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
 * @author jaurambault
 */
class TargetFileVisitor extends SimpleFileVisitor<Path> {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TargetFileVisitor.class);

    final List<FileMatch> targetMatches;
    
    private final FilePattern targetFilePattern;
    private final FileType fileType;
    private final Path targetDirectory;

    public TargetFileVisitor(FileType fileType, Path targetDirectory) {
        this.fileType = fileType;
        this.targetFilePattern = fileType.getTargetFilePattern();
        this.targetDirectory = targetDirectory;
        this.targetMatches = new ArrayList<>();
    }

    public List<FileMatch> getTargetMatches() {
        return targetMatches;
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

        Path relativePath = targetDirectory.relativize(file);

        Matcher matcher = targetFilePattern.getPattern().matcher(relativePath.toString());

        if (matcher.matches()) {

            FileMatch fileMatch = new FileMatch();
            fileMatch.setFileType(fileType);
            fileMatch.setTarget(true);
            fileMatch.setPath(file);

            for (String group : targetFilePattern.getGroups()) {
                fileMatch.addProperty(group, matcher.group(group));
            }

            if (logger.isDebugEnabled()) {
                logger.debug("target name: " + relativePath.toString() + " with src name: " + fileMatch.getSourcePath());
            }

            targetMatches.add(fileMatch);
        }

        return CONTINUE;
    }
}
