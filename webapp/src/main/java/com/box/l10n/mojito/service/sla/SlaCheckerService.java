package com.box.l10n.mojito.service.sla;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.SlaIncident;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.sla.email.SlaCheckerEmailService;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.utils.DateTimeUtils;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jeanaurambault
 */
@Component
public class SlaCheckerService {

  static Logger logger = LoggerFactory.getLogger(SlaCheckerService.class);

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired SlaIncidentRepository slaIncidentRepository;

  @Autowired SlaCheckerEmailService slaCheckerEmailService;

  @Autowired DateTimeUtils dateTimeUtils;

  void checkForIncidents() {
    logger.debug("Checking for SLA incident");
    SlaIncident openIncident = slaIncidentRepository.findFirstByClosedDateIsNull();

    if (openIncident != null) {
      checkWithOpenIncident(openIncident);
    } else {
      checkWithNoOpenIncident();
    }
  }

  void checkWithOpenIncident(SlaIncident openIncident) {
    logger.debug("An incident is open, check for status update");
    List<Repository> ooslaRepositories =
        repositoryRepository
            .findByDeletedFalseAndCheckSLATrueAndRepositoryStatisticOoslaTextUnitCountGreaterThanOrderByNameAsc(
                0L);

    if (ooslaRepositories.isEmpty()) {
      logger.debug("No repositories out of SLA, close incident and send email");
      closeIncidents();
      slaCheckerEmailService.sendCloseIncidentEmail(openIncident.getId());
    } else {
      if (slaCheckerEmailService.shouldResendEmail(openIncident.getCreatedDate())) {
        logger.debug("Still ouf of SLA, resending email");
        slaCheckerEmailService.sendOpenIncidentEmail(openIncident.getId(), ooslaRepositories);
      } else {
        logger.debug("Still ouf of SLA, no need to resend email for now");
      }
    }
  }

  void checkWithNoOpenIncident() {
    logger.debug("No incident open, check if new OOSLA repositories appeared");
    List<Repository> ooslaRepositories =
        repositoryRepository
            .findByDeletedFalseAndCheckSLATrueAndRepositoryStatisticOoslaTextUnitCountGreaterThanOrderByNameAsc(
                0L);

    if (ooslaRepositories.isEmpty()) {
      logger.debug("No repositories are out of SLA");
    } else {
      SlaIncident createIncident = createIncident(ooslaRepositories);
      slaCheckerEmailService.sendOpenIncidentEmail(createIncident.getId(), ooslaRepositories);
    }
  }

  SlaIncident createIncident(List<Repository> ooslaRepositories) {
    logger.debug("Create incidents");
    SlaIncident ooslaIncident = new SlaIncident();
    ooslaIncident.setRepositories(new LinkedHashSet<>(ooslaRepositories));
    return slaIncidentRepository.save(ooslaIncident);
  }

  /**
   * There should be only 1 open incident but since this is not enforced with a DB constraint we
   * close potential extra records.
   */
  @Transactional
  void closeIncidents() {
    logger.debug("Close incidents (there should be only one)");
    ZonedDateTime closedDate = dateTimeUtils.now();
    List<SlaIncident> findByIsNullClosedDate = slaIncidentRepository.findByClosedDateIsNull();
    for (SlaIncident ooSLAIncident : findByIsNullClosedDate) {
      ooSLAIncident.setClosedDate(closedDate);
    }
    slaIncidentRepository.saveAll(findByIsNullClosedDate);
  }
}
