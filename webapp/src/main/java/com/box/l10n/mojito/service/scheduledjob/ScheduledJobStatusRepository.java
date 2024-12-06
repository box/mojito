package com.box.l10n.mojito.service.scheduledjob;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ScheduledJobStatusRepository
    extends JpaRepository<com.box.l10n.mojito.entity.ScheduledJobStatus, Long> {
  @Query(
      "SELECT sjs FROM com.box.l10n.mojito.entity.ScheduledJobStatus sjs WHERE sjs.jobStatus = :jobStatus")
  com.box.l10n.mojito.entity.ScheduledJobStatus findByEnum(ScheduledJobStatus jobStatus);
}
