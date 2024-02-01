package com.box.l10n.mojito.cli.filefinder.file;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author jaurambault
 */
public class FileTypesTest {

  @Test
  public void testValueOf() {
    String name = "ANDROID_STRINGS";
    FileTypes expResult = FileTypes.ANDROID_STRINGS;
    FileTypes result = FileTypes.valueOf(name);
    assertEquals(expResult, result);
  }

  @Test
  public void testToFileType() {
    FileTypes fileTypes = FileTypes.PROPERTIES;
    FileType toFileType = fileTypes.toFileType();
    assertEquals(PropertiesFileType.class, toFileType.getClass());
  }
}
