package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import java.util.Optional;
import org.quartz.JobKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {

  @Query("SELECT sj FROM ScheduledJob sj WHERE sj.uuid = :uuid AND sj.jobType.jobType = :jobType")
  ScheduledJob findByIdAndJobType(String uuid, ScheduledJobType jobType);

  @Query("SELECT sj FROM ScheduledJob sj WHERE sj.uuid = :uuid")
  Optional<ScheduledJob> findByUuid(String uuid);

  default ScheduledJob findByJobKey(JobKey jobKey) {
    return findByIdAndJobType(jobKey.getName(), ScheduledJobType.valueOf(jobKey.getGroup()));
  }
}
