package com.box.l10n.mojito.service.assetintegritychecker;

import com.box.l10n.mojito.entity.AssetIntegrityChecker;
import com.box.l10n.mojito.entity.Repository;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author aloison
 */
@RepositoryRestResource(exported = false)
public interface AssetIntegrityCheckerRepository extends JpaRepository<AssetIntegrityChecker, Long> {

    Set<AssetIntegrityChecker> findByRepository(Repository repository);

    Set<AssetIntegrityChecker> findByRepositoryAndAssetExtension(Repository repository, String assetExtension);

    void deleteByRepository(Repository repository);
}
