package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.entity.UpdateStatistics;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author jyi
 */
@RepositoryRestResource(exported = false)
public interface UpdateStatisticsRepository extends JpaRepository<UpdateStatistics, Long> {

    @Query(value = "select s.repository.id from UpdateStatistics s")
    public Set<Long> findRepositoryIds();

    public List<UpdateStatistics> findByRepositoryIdAndTimeToUpdateBefore(Long repositoryId, DateTime timeToUpdate);

}
