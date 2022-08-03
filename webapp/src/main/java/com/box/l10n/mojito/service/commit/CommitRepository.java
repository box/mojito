package com.box.l10n.mojito.service.commit;

import com.box.l10n.mojito.entity.Commit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface CommitRepository extends JpaRepository<Commit, Long>, JpaSpecificationExecutor<Commit> {
    Optional<Commit> findByNameAndRepositoryId(String name, Long repositoryId);

    @Query(value = "select c from Commit c " +
            "where c.name in :commitNames and c.repository.id = :repositoryId " +
            "and c.commitToPushRun.pushRun.repository.id = :repositoryId order by c.commitToPushRun.createdDate desc ")
    List<Commit> findLatestPushedCommits(@Param("commitNames") List<String> commitNames,
                                         @Param("repositoryId") Long repositoryId,
                                         Pageable pageable);

    @Query(value = "select c from Commit c " +
            "where c.name in :commitNames and c.repository.id = :repositoryId " +
            "and c.commitToPullRun.pullRun.repository.id = :repositoryId order by c.commitToPullRun.createdDate desc ")
    List<Commit> findLatestPulledCommits(@Param("commitNames") List<String> commitNames,
                                         @Param("repositoryId") Long repositoryId,
                                         Pageable pageable);
}
