package com.box.l10n.mojito.service.repository;

import com.box.l10n.mojito.entity.Repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author wyau
 */
@RepositoryRestResource(exported = false)
public interface RepositoryRepository extends JpaRepository<Repository, Long>, JpaSpecificationExecutor<Repository> {
    public Repository findByName(@Param("name") String name);
    public List<Repository> findByDeletedFalseOrderByNameAsc();
}
