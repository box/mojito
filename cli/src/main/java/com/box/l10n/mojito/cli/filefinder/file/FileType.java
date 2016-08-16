package com.box.l10n.mojito.cli.filefinder.file;

import com.box.l10n.mojito.cli.filefinder.FilePattern;
import com.box.l10n.mojito.cli.filefinder.locale.LocaleType;

/**
 * Provides information about a file type: extension, directory layout, source
 * and target file pattern, etc.
 *
 * @author jaurambault
 */
public abstract class FileType {

    String fileExtension;
    String baseNamePattern;
    String sourceFilePatternTemplate;
    String targetFilePatternTemplate;
    LocaleType localeType;
    String parentPath = "(?:.+/)?";
    String subPath = "(?:.+/)?";

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getBaseNamePattern() {
        return baseNamePattern;
    }

    public void setBaseNamePattern(String baseNamePattern) {
        this.baseNamePattern = baseNamePattern;
    }

    public String getSourceFilePatternTemplate() {
        return sourceFilePatternTemplate;
    }

    public void setSourceFilePatternTemplate(String sourceFilePatternTemplate) {
        this.sourceFilePatternTemplate = sourceFilePatternTemplate;
    }

    public String getTargetFilePatternTemplate() {
        return targetFilePatternTemplate;
    }

    public void setTargetFilePatternTemplate(String targetFilePatternTemplate) {
        this.targetFilePatternTemplate = targetFilePatternTemplate;
    }

    public LocaleType getLocaleType() {
        return localeType;
    }

    public void setLocaleType(LocaleType localeType) {
        this.localeType = localeType;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getSubPath() {
        return subPath;
    }

    public void setSubPath(String subPath) {
        this.subPath = subPath;
    }

    public FilePattern getSourceFilePattern() {
        return new FilePattern(this, true);
    }

    public FilePattern getTargetFilePattern() {
        return new FilePattern(this, false);
    }

}
