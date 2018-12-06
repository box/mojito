package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.slf4j.LoggerFactory.getLogger;

public class BranchServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = getLogger(BranchServiceTest.class);

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    BranchService branchService;

    @Autowired
    RepositoryService repositoryService;

    @Test
    public void createBranch() throws RepositoryNameAlreadyUsedException {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        Branch master = branchService.createBranch(repository, "master", null);

        Branch fromFind = branchRepository.findByNameAndRepository("master", repository);
        assertEquals("master", fromFind.getName());
        assertEquals(repository.getId(), fromFind.getRepository().getId());
    }

    @Test
    public void getOrCreateBranch() throws RepositoryNameAlreadyUsedException {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Branch before = branchRepository.findByNameAndRepository("master", repository);
        assertNull(before);

        Branch create = branchService.getOrCreateBranch(repository, "master", null);
        assertEquals("master", create.getName());
        assertEquals(repository.getId(), create.getRepository().getId());

        Branch fromFind = branchRepository.findByNameAndRepository("master", repository);
        assertEquals("master", fromFind.getName());
        assertEquals(repository.getId(), fromFind.getRepository().getId());

        Branch get = branchService.getOrCreateBranch(repository, "master", null);
        assertEquals(create.getId(), get.getId());
    }

    @Test
    public void getOrCreateBranchNameNull() throws RepositoryNameAlreadyUsedException {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String branchName = null;

        Branch before = branchRepository.findByNameAndRepository(branchName, repository);
        assertNull(before);

        Branch create = branchService.getOrCreateBranch(repository, branchName, null);
        assertEquals(branchName, create.getName());
        assertEquals(repository.getId(), create.getRepository().getId());

        Branch fromFind = branchRepository.findByNameAndRepository(branchName, repository);
        assertEquals(branchName, fromFind.getName());
        assertEquals(repository.getId(), fromFind.getRepository().getId());

        Branch get = branchService.getOrCreateBranch(repository, branchName, null);
        assertEquals(create.getId(), get.getId());
    }

}