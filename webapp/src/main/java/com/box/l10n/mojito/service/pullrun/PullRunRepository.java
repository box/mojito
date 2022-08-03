package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.PullRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface PullRunRepository extends JpaRepository<PullRun, Long> {
    Optional<PullRun> findByName(String name);

    @Query(value = "select p from PullRun p " +
            "join CommitToPullRun cpr on cpr.pullRun = p " +
            "join Commit c on c = cpr.commit " +
            "where c.name in :commitNames and c.repository.id = :repositoryId " +
            "and p.repository.id = :repositoryId order by p.createdDate desc ")
    List<PullRun> findLatestByCommitNames(@Param("commitNames") List<String> commitNames,
                                     @Param("repositoryId") Long repositoryId,
                                     Pageable pageable);
}