package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface BranchRepository
    extends JpaRepository<Branch, Long>, JpaSpecificationExecutor<Branch> {
  Branch findByNameAndRepository(String name, Repository repository);

  List<Branch> findByRepositoryIdAndDeletedFalseAndNameNotNullAndNameNot(
      Long repositoryId, String primaryBranch);

  List<Branch> findByDeletedFalseAndNameNotNullAndNameNot(String primaryBranch);
}
