package com.box.l10n.mojito.service.drop.importer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author jaurambault
 */
public class BoxDropImporterTest {

  @Test
  public void testGetLangFromFileName() {
    BoxDropImporter boxDropImporter = new BoxDropImporter(null);
    String langFromFileName = boxDropImporter.getBcp47TagFromFileName("fr-FR_08-26-14.csv");
    assertEquals("fr-FR", langFromFileName);
  }
}
