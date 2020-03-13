package com.box.l10n.mojito.service.repository;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

import static com.box.l10n.mojito.rest.repository.RepositorySpecification.deletedEquals;
import static com.box.l10n.mojito.rest.repository.RepositorySpecification.nameEquals;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.data.jpa.domain.Specification.where;

/**
 * @author aloison
 */
public class RepositoryServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = getLogger(RepositoryServiceTest.class);

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    RepositoryLocaleRepository repositoryLocaleRepository;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    LocaleService localeService;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Transactional
    @Test
    public void testAddSupportedLocales() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        assertEquals(1, repository.getRepositoryLocales().size());

        repositoryService.addRepositoryLocale(repository, "fr-FR");

        assertEquals(2, repository.getRepositoryLocales().size());
    }

    @Test
    public void testAddRootLocaleUnique() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        try {
            repositoryService.addRootLocale(repository, localeService.getDefaultLocale());
            Assert.fail("It shouldn't be possible to add twice a root locale (root locale was added during repository creation)");
        } catch (RuntimeException re) {
            assertEquals("Root locale already exists in repository [" + repository.getId() + "]", re.getMessage());
        }
    }

    @Test
    public void testUpdateRepositoryLocalesForEmptySet() throws Exception {
        String repositoryName = testIdWatcher.getEntityName("repository");
        Repository repository = repositoryService.createRepository(repositoryName);
        Repository repository2 = repositoryService.createRepository(repositoryName + "_2");

        repositoryService.updateRepositoryLocales(new HashSet<RepositoryLocale>());

        assertEquals(1, repositoryRepository.findByName(repository.getName()).getRepositoryLocales().size());
        assertEquals(1, repositoryRepository.findByName(repository2.getName()).getRepositoryLocales().size());
    }

    @Test
    public void testUpdateRepositoryLocales() throws Exception {
        String repositoryName = testIdWatcher.getEntityName("repository");
        Repository repository = repositoryService.createRepository(repositoryName);
        Repository controlledRepository = repositoryService.createRepository(repositoryName + "_2");

        HashSet<RepositoryLocale> repositoryLocales = new HashSet<>();
        repositoryLocales.add(new RepositoryLocale(repository, localeService.findByBcp47Tag("fr-FR"), true, null));

        logger.debug("Updating RepositoryLocale with a Set that only contains 'fr-FR'");
        repositoryService.updateRepositoryLocales(repository, repositoryLocales);

        assertEquals("This number includes the root locale", 2, repositoryRepository.findByName(repository.getName()).getRepositoryLocales().size());
        assertEquals("Repository is not expected to be changed", 1, repositoryRepository.findByName(controlledRepository.getName()).getRepositoryLocales().size());

        repositoryLocales.clear();
        RepositoryLocale zhTW = new RepositoryLocale(repository, localeService.findByBcp47Tag("zh-TW"), true, null);
        repositoryLocales.add(zhTW);
        repositoryLocales.add(new RepositoryLocale(repository, localeService.findByBcp47Tag("zh-HK"), true, zhTW));

        logger.debug("Updating RepositoryLocale again with a new Set that does not contain 'fr-FR'");
        repositoryService.updateRepositoryLocales(repository, repositoryLocales);

        Repository repository1 = repositoryRepository.findByName(repository.getName());
        assertEquals("This number includes the root locale", 3, repository1.getRepositoryLocales().size());
        for (RepositoryLocale repositoryLocale : repository1.getRepositoryLocales()) {
            assertNotEquals("fr-FR should have been removed", "fr-FR", repositoryLocale.getLocale().getBcp47Tag());
        }

        assertEquals("Repository is not expected to be changed", 1, repositoryRepository.findByName(controlledRepository.getName()).getRepositoryLocales().size());
    }
    
    @Test
    public void testDeleteRepository() throws Exception {

        String repositoryName = testIdWatcher.getEntityName("repository");
        Repository repositoryToDelete = repositoryService.createRepository(repositoryName);                
        assertEquals("The repository should be found because it is not deleted yet", 1, 
                repositoryRepository.findAll(
                where(ifParamNotNull(nameEquals(repositoryName))).and(deletedEquals(false))).size()
        );
        Long repositoryId = repositoryToDelete.getId();
        long numberOfRepositories = repositoryRepository.findByDeletedFalseOrderByNameAsc().size();
        
        repositoryService.deleteRepository(repositoryToDelete);
        assertEquals("The deleted repository is found", 0, 
                repositoryRepository.findAll(
                where(ifParamNotNull(nameEquals(repositoryName))).and(deletedEquals(false))).size()
        );
        assertEquals(numberOfRepositories - 1, repositoryRepository.findByDeletedFalseOrderByNameAsc().size());
        assertNull("Found the deleted repository", repositoryRepository.findByName(repositoryName));
        Repository deletedRepository = repositoryRepository.findById(repositoryId).orElse(null);
        assertTrue("The repository is not deleted", deletedRepository.getDeleted());
        assertTrue("The deleted repository is not renamed properly", deletedRepository.getName().startsWith("deleted__"));
        
        // should be able to create repository with the same name
        Repository repositoryToCreateAgain = repositoryService.createRepository(repositoryName);
        assertEquals("The repository should be found when it is added again", 1, 
                repositoryRepository.findAll(
                where(ifParamNotNull(nameEquals(repositoryName))).and(deletedEquals(false))).size()
        );
        assertEquals(numberOfRepositories, repositoryRepository.findByDeletedFalseOrderByNameAsc().size());
        assertEquals("The repository is created with unexpected name", repositoryName, repositoryToCreateAgain.getName());
        assertNotEquals("The repository is created with existing id", repositoryId, repositoryToCreateAgain.getId());
        assertFalse("The repository is deleted", repositoryToCreateAgain.getDeleted());
        
    }
}
