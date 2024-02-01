package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author garion
 */
public class PullRunServiceTest extends ServiceTestBase {

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired PullRunService pullRunService;

  @Autowired RepositoryService repositoryService;

  @Test
  public void testGetOrCreate() throws RepositoryNameAlreadyUsedException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String pullRunName = "testCreatePullRun";

    PullRun pullRun = pullRunService.getOrCreate(pullRunName, repository);

    Assert.assertNotNull(pullRun);
    Assert.assertEquals(pullRunName, pullRun.getName());
  }
}
