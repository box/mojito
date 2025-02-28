package com.box.l10n.mojito.rest.drop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.apiclient.DropWsApi;
import com.box.l10n.mojito.apiclient.PollableTaskClient;
import com.box.l10n.mojito.apiclient.RepositoryClient;
import com.box.l10n.mojito.apiclient.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.apiclient.model.ExportDropConfig;
import com.box.l10n.mojito.apiclient.model.ImportDropConfig;
import com.box.l10n.mojito.apiclient.model.ImportXliffBody;
import com.box.l10n.mojito.apiclient.model.PollableTask;
import com.box.l10n.mojito.apiclient.model.Repository;
import com.box.l10n.mojito.apiclient.model.RepositoryLocale;
import com.box.l10n.mojito.apiclient.model.RepositoryLocaleRepository;
import com.box.l10n.mojito.apiclient.model.RepositoryRepository;
import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.box.l10n.mojito.test.category.IntegrationTest;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author aloison
 */
public class DropWSTest extends WSTestBase {

  /** logger */
  static Logger logger = getLogger(DropWSTest.class);

  @Autowired RepositoryClient repositoryClient;

  @Autowired DropWsApi dropClient;

  @Autowired PollableTaskClient pollableTaskClient;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Test
  @Category({IntegrationTest.class})
  public void testExportDrop() throws InterruptedException, ResourceNotCreatedException {

    List<String> bcp47Tags = Arrays.asList("fr-FR", "ja-JP");

    List<RepositoryLocale> repositoryLocales = getRepositoryLocales(bcp47Tags);
    Repository repoToCreate = new Repository();
    repoToCreate.setName(testIdWatcher.getEntityName("repository"));
    repoToCreate.setDescription(null);
    repoToCreate.setSourceLocale(null);
    repoToCreate.setRepositoryLocales(repositoryLocales);
    repoToCreate.setAssetIntegrityCheckers(null);
    repoToCreate.setCheckSLA(null);
    RepositoryRepository repository = repositoryClient.createRepository(repoToCreate);

    ExportDropConfig exportDropConfig = new ExportDropConfig();
    exportDropConfig.setRepositoryId(repository.getId());
    exportDropConfig.setLocales(bcp47Tags);

    exportDropConfig = dropClient.exportDrop(exportDropConfig);

    pollableTaskClient.waitForPollableTask(exportDropConfig.getPollableTask().getId(), 20000L);

    for (RepositoryLocaleRepository repositoryLocale : repository.getRepositoryLocales()) {
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

    List<RepositoryLocale> repositoryLocales = getRepositoryLocales(bcp47Tags);
    Repository repoToCreate = new Repository();
    repoToCreate.setName(testIdWatcher.getEntityName("repository"));
    repoToCreate.setDescription(null);
    repoToCreate.setSourceLocale(null);
    repoToCreate.setRepositoryLocales(repositoryLocales);
    repoToCreate.setAssetIntegrityCheckers(null);
    repoToCreate.setCheckSLA(null);
    RepositoryRepository repository = repositoryClient.createRepository(repoToCreate);

    ExportDropConfig exportDropConfig = new ExportDropConfig();
    exportDropConfig.setRepositoryId(repository.getId());
    exportDropConfig.setLocales(bcp47Tags);

    exportDropConfig = dropClient.exportDrop(exportDropConfig);

    pollableTaskClient.waitForPollableTask(exportDropConfig.getPollableTask().getId());

    com.box.l10n.mojito.apiclient.model.ImportDropConfig importDropConfig =
        new com.box.l10n.mojito.apiclient.model.ImportDropConfig();
    importDropConfig.setRepositoryId(repository.getId());
    importDropConfig.setDropId(exportDropConfig.getDropId());
    importDropConfig.setStatus(null);
    ImportDropConfig importDropResult = dropClient.importDrop(importDropConfig);

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

    List<RepositoryLocale> repositoryLocales = getRepositoryLocales(bcp47Tags);
    Repository repoToCreate = new Repository();
    repoToCreate.setName(testIdWatcher.getEntityName("repository"));
    repoToCreate.setDescription(null);
    repoToCreate.setSourceLocale(null);
    repoToCreate.setRepositoryLocales(repositoryLocales);
    repoToCreate.setAssetIntegrityCheckers(null);
    repoToCreate.setCheckSLA(null);
    RepositoryRepository repository = repositoryClient.createRepository(repoToCreate);

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

    com.box.l10n.mojito.apiclient.model.ImportXliffBody importXliffBody = new ImportXliffBody();

    importXliffBody.setRepositoryId(Preconditions.checkNotNull(repository.getId()));
    importXliffBody.setTranslationKit(false);
    importXliffBody.setImportStatus(null);
    importXliffBody.setXliffContent(xliffWithTranslationForNonExistingTextUnit);
    String importedXliff = dropClient.importXliff(importXliffBody).getXliffContent();

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
