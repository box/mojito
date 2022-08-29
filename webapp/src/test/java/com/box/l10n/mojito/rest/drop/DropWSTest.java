package com.box.l10n.mojito.rest.drop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.rest.client.DropClient;
import com.box.l10n.mojito.rest.client.PollableTaskClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.rest.entity.ExportDropConfig;
import com.box.l10n.mojito.rest.entity.ImportDropConfig;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.box.l10n.mojito.test.category.IntegrationTest;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/** @author aloison */
public class DropWSTest extends WSTestBase {

  /** logger */
  static Logger logger = getLogger(DropWSTest.class);

  @Autowired RepositoryClient repositoryClient;

  @Autowired DropClient dropClient;

  @Autowired PollableTaskClient pollableTaskClient;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Test
  @Category({IntegrationTest.class})
  public void testExportDrop() throws InterruptedException, ResourceNotCreatedException {

    List<String> bcp47Tags = Arrays.asList("fr-FR", "ja-JP");

    Set<RepositoryLocale> repositoryLocales = getRepositoryLocales(bcp47Tags);
    Repository repository =
        repositoryClient.createRepository(
            testIdWatcher.getEntityName("repository"), null, null, repositoryLocales, null, null);

    ExportDropConfig exportDropConfig = new ExportDropConfig();
    exportDropConfig.setRepositoryId(repository.getId());
    exportDropConfig.setBcp47Tags(bcp47Tags);

    exportDropConfig = dropClient.exportDrop(exportDropConfig);

    pollableTaskClient.waitForPollableTask(exportDropConfig.getPollableTask().getId(), 20000L);

    for (RepositoryLocale repositoryLocale : repository.getRepositoryLocales()) {
      if (repositoryLocale.getParentLocale() != null) {
        assertTrue(bcp47Tags.contains(repositoryLocale.getLocale().getBcp47Tag()));
      } else {
        assertEquals("en", repositoryLocale.getLocale().getBcp47Tag());
      }
    }
  }

  @Test
  @Category({IntegrationTest.class})
  public void testImportDrop() throws Exception {

    List<String> bcp47Tags = Arrays.asList("fr-FR", "ja-JP");

    Set<RepositoryLocale> repositoryLocales = getRepositoryLocales(bcp47Tags);
    Repository repository =
        repositoryClient.createRepository(
            testIdWatcher.getEntityName("repository"), null, null, repositoryLocales, null, null);

    ExportDropConfig exportDropConfig = new ExportDropConfig();
    exportDropConfig.setRepositoryId(repository.getId());
    exportDropConfig.setBcp47Tags(bcp47Tags);

    exportDropConfig = dropClient.exportDrop(exportDropConfig);

    pollableTaskClient.waitForPollableTask(exportDropConfig.getPollableTask().getId());

    ImportDropConfig importDropResult =
        dropClient.importDrop(repository, exportDropConfig.getDropId(), null);

    PollableTask importTask = importDropResult.getPollableTask();
    pollableTaskClient.waitForPollableTask(importTask.getId(), 20000L);

    assertEquals(importDropResult.getDropId(), exportDropConfig.getDropId());
  }

  /**
   * Just calls the import logic with an XLIFF that contains a single translation that is not valid.
   * The expectation is that the text unit will be skipped and flagged for review. No translation
   * will be imported in the project.
   *
   * @throws Exception
   */
  @Test
  @Category({IntegrationTest.class})
  public void testImportDropXliff() throws Exception {

    List<String> bcp47Tags = Arrays.asList("fr-FR", "ja-JP");

    Set<RepositoryLocale> repositoryLocales = getRepositoryLocales(bcp47Tags);
    Repository repository =
        repositoryClient.createRepository(
            testIdWatcher.getEntityName("repository"), null, null, repositoryLocales, null, null);

    String xliffWithTranslationForNonExistingTextUnit =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
            + "<file original=\"en.properties\" source-language=\"en\" target-language=\"fr-FR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
            + "<body>\n"
            + "<trans-unit id=\"\" resname=\"fake\">\n"
            + "<source xml:lang=\"en\">fake</source>\n"
            + "<target xml:lang=\"en\">fake</target>\n"
            + "</trans-unit>\n"
            + "</body>\n"
            + "</file>\n"
            + "</xliff>";

    String importedXliff =
        dropClient.importXiff(
            xliffWithTranslationForNonExistingTextUnit, repository.getId(), false, null);

    String expectedXliff =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
            + "<file original=\"en.properties\" source-language=\"en\" target-language=\"fr-FR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
            + "<body>\n"
            + "<trans-unit id=\"\" resname=\"fake\">\n"
            + "<source xml:lang=\"en\">fake</source>\n"
            + "<target xml:lang=\"fr-FR\" state=\"needs-translation\">fake</target>\n"
            + "<note annotates=\"target\" from=\"automation\">MUST REVIEW\n"
            + "[ERROR] Text unit for id: , Skipping it...</note>\n"
            + "</trans-unit>\n"
            + "</body>\n"
            + "</file>\n"
            + "</xliff>\n"
            + "";

    assertEquals(
        "The text unit doesn't exist in the repository hence must be marked as skipped",
        expectedXliff,
        importedXliff);
  }
}
