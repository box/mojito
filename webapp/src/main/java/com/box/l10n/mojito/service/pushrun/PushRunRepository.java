package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.Repository;
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
public interface PushRunRepository extends JpaRepository<PushRun, Long> {
    Optional<PushRun> findByNameAndRepository(String name, Repository repository);

    @Query(value = "select p from PushRun p " +
            "join CommitToPushRun cpr on cpr.pushRun = p " +
            "join Commit c on c = cpr.commit " +
            "where c.name in :commitNames and c.repository.id = :repositoryId " +
            "and p.repository.id = :repositoryId order by p.createdDate desc ")
    List<PushRun> findLatestByCommitNames(@Param("commitNames") List<String> commitNames,
                                     @Param("repositoryId") Long repositoryId,
                                     Pageable pageable);
}