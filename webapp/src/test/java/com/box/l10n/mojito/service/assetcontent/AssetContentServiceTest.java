package com.box.l10n.mojito.service.assetcontent;

import static com.box.l10n.mojito.service.assetcontent.S3ContentService.FILE_EXTENSION;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class AssetContentServiceTest extends ServiceTestBase {

  /** logger */
  static Logger logger = getLogger(AssetContentServiceTest.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired AssetContentService assetContentService;

  @Autowired AssetContentRepository assetContentRepository;

  @Autowired AssetService assetService;

  @Autowired RepositoryService repositoryService;

  @Autowired BranchService branchService;

  @Test
  public void createAssetContentAndFind() throws RepositoryNameAlreadyUsedException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    Asset fortest1 = assetService.createAsset(repository.getId(), "fortest1", false);

    assertTrue(
        assetContentRepository
            .findByAssetRepositoryIdAndBranchName(repository.getId(), null)
            .isEmpty());

    AssetContent content1 = assetContentService.createAssetContent(fortest1, "fortest1-content1");
    List<AssetContent> assetContents =
        assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), null);
    assertEquals(1L, assetContents.size());
    assertEquals("fortest1-content1", assetContents.get(0).getContent());

    AssetContent content2 = assetContentService.createAssetContent(fortest1, "fortest1-content2");
    assetContents =
        assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), null);
    assertEquals(2L, assetContents.size());
    assertEquals("fortest1-content1", assetContents.get(0).getContent());
    assertEquals("fortest1-content2", assetContents.get(1).getContent());

    AssetContent content2re = assetContentService.createAssetContent(fortest1, "fortest1-content2");
    assetContents =
        assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), null);
    assertEquals(3L, assetContents.size());
    assertEquals("fortest1-content1", assetContents.get(0).getContent());
    assertEquals("fortest1-content2", assetContents.get(1).getContent());
    assertEquals("fortest1-content2", assetContents.get(2).getContent());

    Asset fortest2 = assetService.createAsset(repository.getId(), "fortest2", false);
    AssetContent forTest2Content1 =
        assetContentService.createAssetContent(fortest1, "fortest2-content1");
    assetContents =
        assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), null);
    assertEquals(4L, assetContents.size());
    assertEquals("fortest1-content1", assetContents.get(0).getContent());
    assertEquals("fortest1-content2", assetContents.get(1).getContent());
    assertEquals("fortest1-content2", assetContents.get(2).getContent());
    assertEquals("fortest2-content1", assetContents.get(3).getContent());

    Branch branch1 =
        branchService.getUndeletedOrCreateBranch(
            fortest1.getRepository(), "branch1", null, null, null);
    AssetContent forTest2Content1Branch1 =
        assetContentService.createAssetContent(
            fortest1, "fortest2-content1-branch1", false, branch1);
    assetContents =
        assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), null);
    assertEquals(4L, assetContents.size());
    assetContents =
        assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), "branch1");
    assertEquals(1L, assetContents.size());
    assertEquals("fortest2-content1-branch1", assetContents.get(0).getContent());
  }

  @Test
  public void testCreateAssetContentAndFindOneForS3ContentService()
      throws RepositoryNameAlreadyUsedException {
    final String content = "asset_content_1";
    final String s3PathPrefix = "asset-content";
    final String pathPlaceholder = "%s/%d.%s";

    S3BlobStorage blobStorage = Mockito.mock(S3BlobStorage.class);
    S3ContentService s3ContentService = new S3ContentService(blobStorage, s3PathPrefix);
    AssetContentService s3AssetContentService =
        new AssetContentService(this.branchService, this.assetContentRepository, s3ContentService);

    Repository repository =
        this.repositoryService.createRepository(this.testIdWatcher.getEntityName("repository"));
    Asset asset = this.assetService.createAsset(repository.getId(), "asset_test_1", false);

    assertTrue(
        this.assetContentRepository
            .findByAssetRepositoryIdAndBranchName(repository.getId(), null)
            .isEmpty());

    AssetContent assetContent = s3AssetContentService.createAssetContent(asset, content);

    verify(blobStorage, times(1))
        .put(
            String.format(pathPlaceholder, s3PathPrefix, assetContent.getId(), FILE_EXTENSION),
            content);

    when(blobStorage.getString(anyString())).thenReturn(of(content));

    assetContent = s3AssetContentService.findOne(assetContent.getId());

    assertEquals(content, assetContent.getContent());
    verify(blobStorage, times(1))
        .getString(
            String.format(pathPlaceholder, s3PathPrefix, assetContent.getId(), FILE_EXTENSION));
  }

  private AssetContentService getAssetContentService(S3BlobStorage blobStorage) {
    S3ContentService s3ContentService = new S3ContentService(blobStorage, "asset-content");
    S3UploadContentAsyncTask s3UploadContentAsyncTask =
        new S3UploadContentAsyncTask(s3ContentService);
    S3FallbackContentService s3FallbackContentService =
        new S3FallbackContentService(s3ContentService, s3UploadContentAsyncTask);
    return new AssetContentService(
        this.branchService, this.assetContentRepository, s3FallbackContentService);
  }

  @Test
  public void testCreateAssetContentAndFindOneForS3FallbackContentService()
      throws RepositoryNameAlreadyUsedException {
    final String content = "asset_content_1";
    final String s3PathPrefix = "asset-content";
    final String pathPlaceholder = "%s/%d.%s";

    S3BlobStorage blobStorage = Mockito.mock(S3BlobStorage.class);
    AssetContentService s3FallbackAssetContentService = this.getAssetContentService(blobStorage);

    Repository repository =
        this.repositoryService.createRepository(this.testIdWatcher.getEntityName("repository"));
    Asset asset = this.assetService.createAsset(repository.getId(), "asset_test_1", false);

    assertTrue(
        this.assetContentRepository
            .findByAssetRepositoryIdAndBranchName(repository.getId(), null)
            .isEmpty());

    AssetContent assetContent = s3FallbackAssetContentService.createAssetContent(asset, content);

    verify(blobStorage, times(1))
        .put(
            String.format(pathPlaceholder, s3PathPrefix, assetContent.getId(), FILE_EXTENSION),
            content);

    when(blobStorage.getString(anyString())).thenReturn(of(content));

    assetContent = s3FallbackAssetContentService.findOne(assetContent.getId());

    assertEquals(content, assetContent.getContent());
    verify(blobStorage, times(1))
        .getString(
            String.format(pathPlaceholder, s3PathPrefix, assetContent.getId(), FILE_EXTENSION));
  }

  @Test
  public void testFindOneForS3FallbackContentServiceWithNoContent()
      throws RepositoryNameAlreadyUsedException {
    final String content = "asset_content_1";
    final String s3PathPrefix = "asset-content";
    final String pathPlaceholder = "%s/%d.%s";

    S3BlobStorage blobStorage = Mockito.mock(S3BlobStorage.class);
    AssetContentService s3FallbackAssetContentService = this.getAssetContentService(blobStorage);

    Repository repository =
        this.repositoryService.createRepository(this.testIdWatcher.getEntityName("repository"));
    Asset asset = this.assetService.createAsset(repository.getId(), "asset_test_1", false);

    assertTrue(
        this.assetContentRepository
            .findByAssetRepositoryIdAndBranchName(repository.getId(), null)
            .isEmpty());

    AssetContent assetContent = s3FallbackAssetContentService.createAssetContent(asset, content);

    verify(blobStorage, times(1))
        .put(
            String.format(pathPlaceholder, s3PathPrefix, assetContent.getId(), FILE_EXTENSION),
            content);

    when(blobStorage.getString(anyString())).thenReturn(empty());

    assertThrows(
        ContentNotFoundException.class,
        () -> s3FallbackAssetContentService.findOne(assetContent.getId()));

    verify(blobStorage, times(1))
        .getString(
            String.format(pathPlaceholder, s3PathPrefix, assetContent.getId(), FILE_EXTENSION));
  }
}
