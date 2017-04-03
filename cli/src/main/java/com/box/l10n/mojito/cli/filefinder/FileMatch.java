package com.box.l10n.mojito.cli.filefinder;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.SUB_PATH;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.rest.entity.SourceAsset;
import com.google.common.base.Strings;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains a file match from {@link FileFinder} run.
 *
 * @author jaurambault
 */
public class FileMatch implements Comparable<FileMatch> {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(FileMatch.class);

    /**
     * The {@code Path} of file match.
     */
    Path path;

    /**
     * Indicates if the match is for a source file ({@code false}) or a target
     * file ({@code true}).
     */
    boolean target;

    /**
     * File type associated with the file.
     */
    FileType fileType;

    /**
     * A map of properties associated with the match.
     */
    Map<String, String> properties = new HashMap<>();

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    /**
     * Gets the {@code Path} of file match.
     *
     * @return the {@code Path} of file match
     */
    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void setTarget(boolean target) {
        this.target = target;
    }

    /**
     * Gets the source path for this match.
     *
     * @return the source path
     */
    public String getSourcePath() {

        String res = fileType.getSourceFilePatternTemplate();

        res = res.replace(getNamedPlaceholder(FILE_EXTENSION), fileType.getSourceFileExtension());
        res = res.replace(getNamedPlaceholder(PARENT_PATH), getProperty(PARENT_PATH));
        res = res.replace(getNamedPlaceholder(SUB_PATH), getProperty(SUB_PATH));
        res = res.replace(getNamedPlaceholder(BASE_NAME), getProperty(BASE_NAME));
        res = res.replace(getNamedPlaceholder(LOCALE), fileType.getLocaleType().getSourceLocale());

        return res;
    }

    /**
     * Gets the target path for a given locale and this match.
     *
     * @param locale the locale of the target file
     * @return the target path
     */
    public String getTargetPath(String locale) {

        String res = fileType.getTargetFilePatternTemplate();

        locale = fileType.getLocaleType().getTargetLocaleRepresentation(locale);

        res = res.replace(getNamedPlaceholder(FILE_EXTENSION), fileType.getTargetFileExtension());
        res = res.replace(getNamedPlaceholder(PARENT_PATH), getProperty(PARENT_PATH));
        res = res.replace(getNamedPlaceholder(SUB_PATH), getProperty(SUB_PATH));
        res = res.replace(getNamedPlaceholder(BASE_NAME), getProperty(BASE_NAME));
        res = res.replace(getNamedPlaceholder(LOCALE), locale);

        return res;
    }

    /**
     * Gets the placeholder string for a given placeholder name.
     *
     * @param name placeholder name
     * @return the placeholder string
     */
    private String getNamedPlaceholder(String name) {
        return "{" + name + "}";
    }

    /**
     * Adds a property to this file match.
     *
     * @param propertyName
     * @param propertyValue
     */
    public void addProperty(String propertyName, String propertyValue) {
        properties.put(propertyName, propertyValue);
    }

    /**
     * Gets a property for a given property name.
     *
     * @param name the property name
     * @return the property
     */
    private String getProperty(String name) {
        return Strings.nullToEmpty(properties.get(name));
    }

    @Override
    public int compareTo(FileMatch o) {
        return this.path.compareTo(o.getPath());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.path);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FileMatch other = (FileMatch) obj;
        if (!Objects.equals(this.path, other.path)) {
            return false;
        }
        return true;
    }

}
