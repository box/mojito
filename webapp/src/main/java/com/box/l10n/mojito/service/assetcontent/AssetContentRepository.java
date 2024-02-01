package com.box.l10n.mojito.service.assetcontent;

import com.box.l10n.mojito.entity.AssetContent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface AssetContentRepository
    extends JpaRepository<AssetContent, Long>, JpaSpecificationExecutor<AssetContent> {

  List<AssetContent> findByAssetRepositoryIdAndBranchName(Long repositoryId, String branchName);

  @Transactional
  int deleteByAssetExtractionsIdIsNull();
}
