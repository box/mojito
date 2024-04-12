package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface BranchRepository
    extends JpaRepository<Branch, Long>, JpaSpecificationExecutor<Branch> {

  @Override
  @EntityGraph(value = "Branch.legacy", type = EntityGraphType.FETCH)
  Optional<Branch> findById(Long aLong);

  Branch findByNameAndRepository(String name, Repository repository);

  List<Branch> findByRepositoryIdAndDeletedFalseAndNameNotNullAndNameNot(
      Long repositoryId, String primaryBranch);

  List<Branch> findByDeletedFalseAndNameNotNullAndNameNot(String primaryBranch);
}
