package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.Repository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface PushRunRepository extends JpaRepository<PushRun, Long> {

  @Override
  @EntityGraph(value = "PushRun.legacy", type = EntityGraphType.FETCH)
  Optional<PushRun> findById(Long aLong);

  Optional<PushRun> findByNameAndRepository(String name, Repository repository);

  @Query(
      value =
          """
              select p from PushRun p
              join CommitToPushRun cpr on cpr.pushRun = p
              join Commit c on c = cpr.commit
              where c.name in :commitNames and c.repository.id = :repositoryId
          and p.repository.id = :repositoryId order by p.createdDate desc
          """)
  List<PushRun> findLatestByCommitNames(
      @Param("commitNames") List<String> commitNames,
      @Param("repositoryId") Long repositoryId,
      Pageable pageable);

  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          delete pr
          from push_run pr
          where pr.created_date < :beforeDate
          """)
  void deleteAllByCreatedDateBefore(@Param("beforeDate") ZonedDateTime beforeDate);
}
