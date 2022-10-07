package com.box.l10n.mojito.service.commit;

import com.box.l10n.mojito.entity.Commit;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.pullrun.PullRunRepository;
import com.box.l10n.mojito.service.pushrun.PushRunRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** @author garion */
public class CommitServiceTest extends ServiceTestBase {

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired CommitRepository commitRepository;

  @Autowired CommitService commitService;

  @Autowired CommitToPushRunRepository commitToPushRunRepository;

  @Autowired PushRunRepository pushRunRepository;

  @Autowired PullRunRepository pullRunRepository;

  @Autowired RepositoryService repositoryService;

  @Test
  public void testGetCommitWithNameAndRepository() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitName = "commitName";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime sourceCreationTime = DateTime.now();

    Commit createdCommit =
        commitService.getOrCreateCommit(
            repository, commitName, authorEmail, authorName, sourceCreationTime);

    Commit retrievedCommit =
        commitService.getCommitWithNameAndRepository(commitName, repository.getId()).orElse(null);

    Assert.assertNotNull(retrievedCommit);
    Assert.assertEquals(createdCommit.getId(), retrievedCommit.getId());
    Assert.assertEquals(createdCommit.getName(), retrievedCommit.getName());
    Assert.assertEquals(createdCommit.getAuthorEmail(), retrievedCommit.getAuthorEmail());
    Assert.assertEquals(createdCommit.getAuthorName(), retrievedCommit.getAuthorName());
  }

  @Test
  public void testSaveCommit() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitName = "commitName";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime sourceCreationTime = DateTime.now();

    Commit createdCommit =
        commitService.getOrCreateCommit(
            repository, commitName, authorEmail, authorName, sourceCreationTime);

    Assert.assertNotNull(createdCommit);
    Assert.assertNotEquals(0, (long) createdCommit.getId());
    Assert.assertEquals(commitName, createdCommit.getName());
    Assert.assertEquals(authorEmail, createdCommit.getAuthorEmail());
    Assert.assertEquals(authorName, createdCommit.getAuthorName());
    Assert.assertEquals(sourceCreationTime, createdCommit.getSourceCreationDate());
  }

  public void testSaveCommitWithIdenticalDataReturnsSameCommit() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitName = "commitName";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime creationTime = DateTime.now();
    Commit commit1 =
        commitService.getOrCreateCommit(
            repository, commitName, authorEmail, authorName, creationTime);
    Commit commit2 =
        commitService.getOrCreateCommit(
            repository, commitName, authorEmail, authorName, creationTime);
    Assert.assertEquals(commit1.getId(), commit2.getId());
  }

  @Test(expected = SaveCommitMismatchedExistingDataException.class)
  public void testSaveCommitDuplicateNameAndRepoWithDifferentDateThrows() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitName = "commitName";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    commitService.getOrCreateCommit(
        repository, commitName, authorEmail, authorName, DateTime.now());
    commitService.getOrCreateCommit(
        repository, commitName, authorEmail, authorName, DateTime.now().plusHours(3));
  }

  @Test
  public void testSaveCommitDuplicateNameAndDifferentRepo() throws Exception {
    Repository repositoryA =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository") + 'A');
    Repository repositoryB =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository") + 'B');

    commitService.getOrCreateCommit(repositoryA, "n1", "ae1", "an1", DateTime.now());
    commitService.getOrCreateCommit(repositoryB, "n1", "ae2", "an2", DateTime.now());
  }

  @Test
  public void testGetLastPushedCommit() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitName = "commitName";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime sourceCreationTime = DateTime.now();

    Commit createdCommit =
        commitService.getOrCreateCommit(
            repository, commitName, authorEmail, authorName, sourceCreationTime);

    Optional<Commit> commitWithoutPush =
        commitService.getLastPushedCommit(ImmutableList.of(commitName), repository.getId());
    Assert.assertFalse(commitWithoutPush.isPresent());

    PushRun pushRun = new PushRun();
    pushRun.setName(UUID.randomUUID().toString());
    pushRun.setRepository(repository);
    pushRunRepository.save(pushRun);

    commitService.associateCommitToPushRun(createdCommit, pushRun);

    Optional<Commit> commitWithPush =
        commitService.getLastPushedCommit(ImmutableList.of(commitName), repository.getId());
    Assert.assertTrue(commitWithPush.isPresent());
    Assert.assertEquals(commitWithPush.get().getId(), createdCommit.getId());
  }

  @Test
  public void testGetLastPushedCommitMultipleEntries() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitNameA = "commitNameA";
    String commitNameB = "commitNameB";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime sourceCreationTime = DateTime.now();

    Commit createdCommitA =
        commitService.getOrCreateCommit(
            repository, commitNameA, authorEmail, authorName, sourceCreationTime);

    Commit createdCommitB =
        commitService.getOrCreateCommit(
            repository, commitNameB, authorEmail, authorName, sourceCreationTime);

    Optional<Commit> commitWithoutPush =
        commitService.getLastPushedCommit(
            ImmutableList.of(commitNameA, commitNameB), repository.getId());

    Assert.assertFalse(commitWithoutPush.isPresent());

    PushRun oldPushRun = new PushRun();
    oldPushRun.setName(UUID.randomUUID().toString());
    oldPushRun.setRepository(repository);
    pushRunRepository.save(oldPushRun);

    PushRun newPushRun = new PushRun();
    newPushRun.setName(UUID.randomUUID().toString());
    newPushRun.setRepository(repository);
    pushRunRepository.save(newPushRun);

    commitService.associateCommitToPushRun(createdCommitB, newPushRun);

    Optional<Commit> commitWithPush =
        commitService.getLastPushedCommit(
            ImmutableList.of(commitNameA, commitNameB), repository.getId());

    Assert.assertTrue(commitWithPush.isPresent());
    Assert.assertEquals(commitWithPush.get().getId(), createdCommitB.getId());
  }

  @Test
  public void testGetLastPushRunMultipleEntries() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitNameA = "commitNameA";
    String commitNameB = "commitNameB";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime sourceCreationTime = DateTime.now();

    Commit createdCommitA =
        commitService.getOrCreateCommit(
            repository, commitNameA, authorEmail, authorName, sourceCreationTime);

    Commit createdCommitB =
        commitService.getOrCreateCommit(
            repository, commitNameB, authorEmail, authorName, sourceCreationTime);

    Optional<Commit> commitWithoutPush =
        commitService.getLastPushedCommit(
            ImmutableList.of(commitNameA, commitNameB), repository.getId());

    Assert.assertFalse(commitWithoutPush.isPresent());

    PushRun oldPushRun = new PushRun();
    oldPushRun.setName(UUID.randomUUID().toString());
    oldPushRun.setRepository(repository);
    pushRunRepository.save(oldPushRun);

    PushRun newPushRun = new PushRun();
    newPushRun.setName(UUID.randomUUID().toString());
    newPushRun.setRepository(repository);
    pushRunRepository.save(newPushRun);

    commitService.associateCommitToPushRun(createdCommitB, newPushRun);

    Optional<PushRun> pushRun =
        commitService.getLastPushRun(
            ImmutableList.of(commitNameA, commitNameB), repository.getId());

    Assert.assertTrue(pushRun.isPresent());
    Assert.assertEquals(pushRun.get().getId(), newPushRun.getId());
  }

  @Test
  public void testGetLastPulledCommit() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitName = "commitName";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime sourceCreationTime = DateTime.now();

    Commit createdCommit =
        commitService.getOrCreateCommit(
            repository, commitName, authorEmail, authorName, sourceCreationTime);

    Optional<Commit> commitWithoutPull =
        commitService.getLastPulledCommit(ImmutableList.of(commitName), repository.getId());
    Assert.assertFalse(commitWithoutPull.isPresent());

    PullRun pullRun = new PullRun();
    pullRun.setName(UUID.randomUUID().toString());
    pullRun.setRepository(repository);
    pullRunRepository.save(pullRun);

    commitService.associateCommitToPullRun(createdCommit, pullRun);

    Optional<Commit> commitWithPull =
        commitService.getLastPulledCommit(ImmutableList.of(commitName), repository.getId());
    Assert.assertTrue(commitWithPull.isPresent());
    Assert.assertEquals(commitWithPull.get().getId(), createdCommit.getId());
  }

  @Test
  public void testGetLastPulledCommitMultipleEntries() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitNameA = "commitNameA";
    String commitNameB = "commitNameB";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime sourceCreationTime = DateTime.now();

    Commit createdCommitA =
        commitService.getOrCreateCommit(
            repository, commitNameA, authorEmail, authorName, sourceCreationTime);

    Commit createdCommitB =
        commitService.getOrCreateCommit(
            repository, commitNameB, authorEmail, authorName, sourceCreationTime);

    Optional<Commit> commitWithoutPull =
        commitService.getLastPulledCommit(
            ImmutableList.of(commitNameA, commitNameB), repository.getId());

    Assert.assertFalse(commitWithoutPull.isPresent());

    PullRun oldPullRun = new PullRun();
    oldPullRun.setName(UUID.randomUUID().toString());
    oldPullRun.setRepository(repository);
    pullRunRepository.save(oldPullRun);

    PullRun newPullRun = new PullRun();
    newPullRun.setName(UUID.randomUUID().toString());
    newPullRun.setRepository(repository);
    pullRunRepository.save(newPullRun);

    commitService.associateCommitToPullRun(createdCommitB, newPullRun);

    Optional<Commit> commitWithPull =
        commitService.getLastPulledCommit(
            ImmutableList.of(commitNameA, commitNameB), repository.getId());

    Assert.assertTrue(commitWithPull.isPresent());
    Assert.assertEquals(commitWithPull.get().getId(), createdCommitB.getId());
  }

  @Test
  public void testGetLastPullRunMultipleEntries() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitNameA = "commitNameA";
    String commitNameB = "commitNameB";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime sourceCreationTime = DateTime.now();

    Commit createdCommitA =
        commitService.getOrCreateCommit(
            repository, commitNameA, authorEmail, authorName, sourceCreationTime);

    Commit createdCommitB =
        commitService.getOrCreateCommit(
            repository, commitNameB, authorEmail, authorName, sourceCreationTime);

    Optional<Commit> commitWithoutPull =
        commitService.getLastPulledCommit(
            ImmutableList.of(commitNameA, commitNameB), repository.getId());

    Assert.assertFalse(commitWithoutPull.isPresent());

    PullRun oldPullRun = new PullRun();
    oldPullRun.setName(UUID.randomUUID().toString());
    oldPullRun.setRepository(repository);
    pullRunRepository.save(oldPullRun);

    PullRun newPullRun = new PullRun();
    newPullRun.setName(UUID.randomUUID().toString());
    newPullRun.setRepository(repository);
    pullRunRepository.save(newPullRun);

    commitService.associateCommitToPullRun(createdCommitB, newPullRun);

    Optional<PullRun> pullRun =
        commitService.getLastPullRun(
            ImmutableList.of(commitNameA, commitNameB), repository.getId());

    Assert.assertTrue(pullRun.isPresent());
    Assert.assertEquals(pullRun.get().getId(), newPullRun.getId());
  }

  @Test
  public void testSetAndGetPushRunForCommit() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitName = "commitName";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime sourceCreationTime = DateTime.now();

    Commit createdCommit =
        commitService.getOrCreateCommit(
            repository, commitName, authorEmail, authorName, sourceCreationTime);

    PushRun pushRun = new PushRun();
    pushRun.setName(UUID.randomUUID().toString());
    pushRun.setRepository(repository);
    pushRunRepository.save(pushRun);

    commitService.associateCommitToPushRun(createdCommit, pushRun);

    Optional<PushRun> retrievedPushRun = commitService.getPushRunForCommitId(createdCommit.getId());
    Assert.assertTrue(retrievedPushRun.isPresent());
    Assert.assertEquals(pushRun.getId(), retrievedPushRun.get().getId());
  }

  @Test
  public void testSetAndGetPushRunForCommitWithIDs() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitName = "commitName";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime sourceCreationTime = DateTime.now();

    Commit createdCommit =
        commitService.getOrCreateCommit(
            repository, commitName, authorEmail, authorName, sourceCreationTime);

    PushRun pushRun = new PushRun();
    pushRun.setName(UUID.randomUUID().toString());
    pushRun.setRepository(repository);
    pushRunRepository.save(pushRun);

    commitService.associateCommitToPushRun(
        repository.getId(), createdCommit.getName(), pushRun.getName());

    Optional<PushRun> retrievedPushRun = commitService.getPushRunForCommitId(createdCommit.getId());
    Assert.assertTrue(retrievedPushRun.isPresent());
    Assert.assertEquals(pushRun.getId(), retrievedPushRun.get().getId());
  }

  @Test
  public void testSetAndGetPullRunForCommit() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitName = "commitName";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime sourceCreationTime = DateTime.now();

    Commit createdCommit =
        commitService.getOrCreateCommit(
            repository, commitName, authorEmail, authorName, sourceCreationTime);

    PullRun pullRun = new PullRun();
    pullRun.setName(UUID.randomUUID().toString());
    pullRun.setRepository(repository);
    pullRunRepository.save(pullRun);

    commitService.associateCommitToPullRun(createdCommit, pullRun);

    Optional<PullRun> retrievedPullRun = commitService.getPullRunForCommitId(createdCommit.getId());
    Assert.assertTrue(retrievedPullRun.isPresent());
    Assert.assertEquals(pullRun.getId(), retrievedPullRun.get().getId());
  }

  @Test
  public void testSetAndGetPullRunForCommitWithIDs() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String commitName = "commitName";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime sourceCreationTime = DateTime.now();

    Commit createdCommit =
        commitService.getOrCreateCommit(
            repository, commitName, authorEmail, authorName, sourceCreationTime);

    PullRun pullRun = new PullRun();
    pullRun.setName(UUID.randomUUID().toString());
    pullRun.setRepository(repository);
    pullRunRepository.save(pullRun);

    commitService.associateCommitToPullRun(
        repository.getId(), createdCommit.getName(), pullRun.getName());

    Optional<PullRun> retrievedPullRun = commitService.getPullRunForCommitId(createdCommit.getId());
    Assert.assertTrue(retrievedPullRun.isPresent());
    Assert.assertEquals(pullRun.getId(), retrievedPullRun.get().getId());
  }
}
