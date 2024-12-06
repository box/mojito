package com.box.l10n.mojito.pagerduty;

import com.box.l10n.mojito.entity.PagerDutyIncident;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface PagerDutyIncidentRepository extends JpaRepository<PagerDutyIncident, Long> {

  @Query(
      "SELECT pdi FROM PagerDutyIncident pdi WHERE pdi.clientName = :clientName AND pdi.dedupKey = :dedupKey AND pdi.resolvedAt IS NULL")
  Optional<PagerDutyIncident> findOpenIncident(String clientName, String dedupKey);
}
