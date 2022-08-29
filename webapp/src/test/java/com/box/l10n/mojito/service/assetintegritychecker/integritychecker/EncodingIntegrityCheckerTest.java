package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.box.l10n.mojito.test.IOTestBase;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class EncodingIntegrityCheckerTest extends IOTestBase {

  @Test(expected = IntegrityCheckException.class)
  public void testEncodingForWrongEncoding() throws IOException {
    String string = getFileAsString("/xliff-latin.xliff");
    EncodingIntegrityChecker encodingIntegrityChecker = new EncodingIntegrityChecker();
    encodingIntegrityChecker.check(string);
  }

  @Test(expected = IntegrityCheckException.class)
  public void testGB18030Encoding() throws IOException {
    String string = getFileAsString("/xliff-gb18030.xliff");
    EncodingIntegrityChecker encodingIntegrityChecker = new EncodingIntegrityChecker();
    encodingIntegrityChecker.check(string);
  }

  @Test
  public void testUTF8Encoding() throws IOException {
    String string = getFileAsString("/xliff-utf8.xliff");
    EncodingIntegrityChecker encodingIntegrityChecker = new EncodingIntegrityChecker();
    encodingIntegrityChecker.check(string);
  }

  private String getFileAsString(String filename) throws IOException {
    File file = new File(getInputResourcesTestDir() + filename);
    return Files.toString(file, StandardCharsets.UTF_8);
  }
}
