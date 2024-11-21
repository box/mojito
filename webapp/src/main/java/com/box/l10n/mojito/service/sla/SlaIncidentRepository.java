package com.box.l10n.mojito.service.sla;

import com.box.l10n.mojito.entity.SlaIncident;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface SlaIncidentRepository extends JpaRepository<SlaIncident, Long> {

  SlaIncident findFirstByClosedDateIsNull();

  List<SlaIncident> findByClosedDateIsNull();
}
