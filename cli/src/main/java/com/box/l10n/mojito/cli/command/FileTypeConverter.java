package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.cli.filefinder.file.FileTypes;
import java.util.Arrays;

/**
 * @author jaurambault
 */
public class FileTypeConverter implements IStringConverter<FileType> {

  @Override
  public FileType convert(String value) {

    FileType fileType = null;

    if (value != null) {
      try {
        fileType =
            com.box.l10n.mojito.cli.filefinder.file.FileTypes.valueOf(value.toUpperCase())
                .toFileType();
      } catch (IllegalArgumentException iae) {
        String msg =
            "Invalid file type ["
                + value
                + "], should be one of: "
                + Arrays.toString(FileTypes.values());
        throw new ParameterException(msg);
      }
    }

    return fileType;
  }
}
