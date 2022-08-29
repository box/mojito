package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.entity.StatisticsSchedule;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** @author jyi */
@RepositoryRestResource(exported = false)
public interface StatisticsScheduleRepository extends JpaRepository<StatisticsSchedule, Long> {

  @Query(value = "select s.repository.id from StatisticsSchedule s")
  public Set<Long> findRepositoryIds();

  public List<StatisticsSchedule> findByRepositoryIdAndTimeToUpdateBefore(
      Long repositoryId, DateTime timeToUpdate);
}
