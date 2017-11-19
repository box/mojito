package com.box.l10n.mojito.cli.command;

import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class to manage source and target directories used by {@link Command} and
 * provides utilities to manipulate files within those directories.
 *
 * @author jaurambault
 */
public class CommandDirectories {

    /**
     * Path to the source directory
     */
    Path sourceDirectoryPath;

    /**
     * Path to the target directory
     */
    Path targetDirectoryPath;

    /**
     * Creates the instance from path string of the source directory (the source
     * directory will be used as target directory).
     *
     * @param sourceDirectoryPathString source directory, if {@code null} the
     * current user directory will be used
     */
    public CommandDirectories(String sourceDirectoryPathString) {
        this(sourceDirectoryPathString, null);
    }

    /**
     * Creates the instance from path strings of the source and target
     * directories.
     *
     * @param sourceDirectoryPathString source directory, if {@code null} the
     * current user directory will be used
     * @param targetDirectoryPathString target directory, if {@code null} the
     * source directory will be used
     */
    public CommandDirectories(String sourceDirectoryPathString, String targetDirectoryPathString) {

        sourceDirectoryPathString = MoreObjects.firstNonNull(sourceDirectoryPathString, System.getProperty("user.dir"));
        targetDirectoryPathString = MoreObjects.firstNonNull(targetDirectoryPathString, sourceDirectoryPathString);

        sourceDirectoryPath = Paths.get(sourceDirectoryPathString).toAbsolutePath();
        targetDirectoryPath = Paths.get(targetDirectoryPathString).toAbsolutePath();
    }

    /**
     * Gets a path that has the same relative path in the target directory as
     * the given path in the source directory.
     *
     * With source directory: {@code "source"}, target directory:
     * {@code "target"} and pathInSourceDirectory: {@code "source/sub/file1"}
     * will return {@code "target/sub/file1"}
     *
     * @param pathInSourceDirectory path relativized in source directory
     * @return path in the target directory
     */
    public Path resolveWithTargetDirectory(Path pathInSourceDirectory) {
        return relativizeWithUserDirectory(targetDirectoryPath.resolve(relativizeWithSourceDirectory(pathInSourceDirectory)));
    }

    /**
     * Gets a path that has the same relative path in the target directory as
     * the given path in the source directory and creates parent directories of
     * the target path if they don't exist.
     *
     * With source directory: {@code "source"}, target directory:
     * {@code "target"} and pathInSourceDirectory: {@code "source/sub/file1"}
     * will return {@code "target/sub/file1"}. {@code "target/sub/"} will be
     * created.
     *
     * @param pathInSourceDirectory path relativized in source directory
     * @return path in the target directory
     */
    public Path resolveWithTargetDirectoryAndCreateParentDirectories(Path pathInSourceDirectory) throws CommandException {

        Path path = resolveWithTargetDirectory(pathInSourceDirectory);

        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
        } catch (IOException ioe) {
            throw new CommandException("Can't create target directory: " + path, ioe);
        }

        return path;
    }

    /**
     * Construct a relative path between the source directory and the given
     * path.
     *
     * @param path the path to relativize against the source directory
     * @return the relative path
     */
    public Path relativizeWithSourceDirectory(Path path) {
        return sourceDirectoryPath.relativize(path.toAbsolutePath());
    }

    /**
     * Construct a relative path between the target directory and the given
     * path.
     *
     * @param path the path to relativize against the target directory
     * @return the relative path
     */
    public Path relativizeWithTargetDirectory(Path path) {
        return targetDirectoryPath.relativize(path.toAbsolutePath());
    }

    /**
     * Construct a relative path between the user directory and the given path.
     *
     * @param path the path to relativize against the user directory
     * @return the relative path
     */
    public Path relativizeWithUserDirectory(Path path) {
        return Paths.get(System.getProperty("user.dir")).relativize(path.toAbsolutePath());
    }

    /**
     * Lists files with a given extension in the source directory directory.
     *
     * @param extensions extension of files to be returned
     * @return list of files with given extension
     * @throws CommandException
     */
    public List<Path> listFilesWithExtensionInSourceDirectory(String... extensions) throws CommandException {

        final List<Path> paths = new ArrayList<>();
        
        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(buildGlobPatternForFileWithExtensions(extensions));

        SimpleFileVisitor<Path> extensionFileVisitor = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                if (pathMatcher.matches(file)) {
                    paths.add(file);
                }

                return FileVisitResult.CONTINUE;
            }
        };

        try {
            Files.walkFileTree(sourceDirectoryPath, extensionFileVisitor);
        } catch (IOException ioe) {
            throw new CommandException("Error while listing the file for extension: " + Arrays.toString(extensions), ioe);
        }

        return paths;
    }

    /**
     * Build a Glob pattern that matches files with given extensions (case 
     * insensitive) across boundaries: glob:**.{ext1,EXT1,ext2,EXT2 ...}.
     * @param extensions extensions to be added in the pattern
     * @return the glob pattern
     */
    String buildGlobPatternForFileWithExtensions(String... extensions) {
        StringBuilder extensionsPattern = new StringBuilder("glob:**.{");
        
        boolean first = true;
        
        for (String extension : extensions) {
            
            if (!first) {
                extensionsPattern.append(",");
            } else {
                first = false;
            }
            
            extensionsPattern.append(extension.toLowerCase()).append(",").append(extension.toUpperCase());
        }

        extensionsPattern.append("}");

        return extensionsPattern.toString();
    }

    public Path getSourceDirectoryPath() {
        return sourceDirectoryPath;
    }

    public Path getTargetDirectoryPath() {
        return targetDirectoryPath;
    }

}
