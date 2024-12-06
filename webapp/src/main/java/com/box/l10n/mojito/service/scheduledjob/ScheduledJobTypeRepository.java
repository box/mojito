package com.box.l10n.mojito.service.scheduledjob;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ScheduledJobTypeRepository
    extends JpaRepository<com.box.l10n.mojito.entity.ScheduledJobType, Long> {
  @Query(
      "SELECT sjt FROM com.box.l10n.mojito.entity.ScheduledJobType sjt WHERE sjt.jobType = :jobType")
  com.box.l10n.mojito.entity.ScheduledJobType findByEnum(
      com.box.l10n.mojito.service.scheduledjob.ScheduledJobType jobType);
}
