package com.box.l10n.mojito.service.branch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

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

public class BranchServiceTest extends ServiceTestBase {

  /** logger */
  static Logger logger = getLogger(BranchServiceTest.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired BranchRepository branchRepository;

  @Autowired BranchService branchService;

  @Autowired RepositoryService repositoryService;

  @Autowired BranchMergeTargetRepository branchMergeTargetRepository;

  @Test
  public void createBranch() throws RepositoryNameAlreadyUsedException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
    Branch master = branchService.createBranch(repository, "master", null, null);

    Branch fromFind = branchRepository.findByNameAndRepository("master", repository);
    assertEquals("master", fromFind.getName());
    assertEquals(repository.getId(), fromFind.getRepository().getId());
  }

  @Test
  public void getOrCreateBranch() throws RepositoryNameAlreadyUsedException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    Branch before = branchRepository.findByNameAndRepository("master", repository);
    assertNull(before);

    Branch create =
        branchService.getUndeletedOrCreateBranch(repository, "master", null, null, null);
    assertEquals("master", create.getName());
    assertEquals(repository.getId(), create.getRepository().getId());

    Branch fromFind = branchRepository.findByNameAndRepository("master", repository);
    assertEquals("master", fromFind.getName());
    assertEquals(repository.getId(), fromFind.getRepository().getId());

    Branch get = branchService.getUndeletedOrCreateBranch(repository, "master", null, null, null);
    assertEquals(create.getId(), get.getId());
  }

  @Test
  public void getOrCreateBranchNameNull() throws RepositoryNameAlreadyUsedException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String branchName = null;

    Branch before = branchRepository.findByNameAndRepository(branchName, repository);
    assertNull(before);

    Branch create =
        branchService.getUndeletedOrCreateBranch(repository, branchName, null, null, null);
    assertEquals(branchName, create.getName());
    assertEquals(repository.getId(), create.getRepository().getId());

    Branch fromFind = branchRepository.findByNameAndRepository(branchName, repository);
    assertEquals(branchName, fromFind.getName());
    assertEquals(repository.getId(), fromFind.getRepository().getId());

    Branch get = branchService.getUndeletedOrCreateBranch(repository, branchName, null, null, null);
    assertEquals(create.getId(), get.getId());
  }

  @Test
  public void branchMergeTarget() throws RepositoryNameAlreadyUsedException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    // Null branch name, don't even attempt it
    Branch branch = branchService.getUndeletedOrCreateBranch(repository, null, null, null, true);
    assertTrue(branchMergeTargetRepository.findByBranch(branch).isEmpty());

    // No branchTarget set
    branch = branchService.getUndeletedOrCreateBranch(repository, "b1", null, null, null);
    assertTrue(branchMergeTargetRepository.findByBranch(branch).isEmpty());

    // Branch target should be true
    branch = branchService.getUndeletedOrCreateBranch(repository, "b2", null, null, true);
    assertTrue(branchMergeTargetRepository.findByBranch(branch).get().isTargetsMain());

    // Branch target should be false
    branch = branchService.getUndeletedOrCreateBranch(repository, "b3", null, null, false);
    assertFalse(branchMergeTargetRepository.findByBranch(branch).get().isTargetsMain());

    // Duplicate but change the merge target - should change the branch target.
    branch = branchService.getUndeletedOrCreateBranch(repository, "b3", null, null, true);
    assertTrue(branchMergeTargetRepository.findByBranch(branch).get().isTargetsMain());
  }
}
