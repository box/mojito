package com.box.l10n.mojito.rest.delta;

import static com.box.l10n.mojito.rest.locale.LocaleSpecification.bcp47TagIn;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import static org.springframework.data.jpa.domain.Specification.where;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.PageView;
import com.box.l10n.mojito.rest.repository.RepositoryWithIdNotFoundException;
import com.box.l10n.mojito.service.delta.DeltaService;
import com.box.l10n.mojito.service.delta.PushRunsMissingException;
import com.box.l10n.mojito.service.delta.dtos.DeltaResponseDTO;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import com.box.l10n.mojito.service.pullrun.PullRunRepository;
import com.box.l10n.mojito.service.pushrun.PushRunRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TextUnitVariantDeltaDTO;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author garion
 */
@RestController
public class DeltaWS {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DeltaWS.class);

  EntityManager entityManager;

  DeltaService deltaService;

  LocaleRepository localeRepository;

  PushRunRepository pushRunRepository;

  PullRunRepository pullRunRepository;

  RepositoryRepository repositoryRepository;

  public DeltaWS(
      EntityManager entityManager,
      DeltaService deltaService,
      LocaleRepository localeRepository,
      PushRunRepository pushRunRepository,
      PullRunRepository pullRunRepository,
      RepositoryRepository repositoryRepository) {
    this.entityManager = entityManager;
    this.deltaService = deltaService;
    this.localeRepository = localeRepository;
    this.pushRunRepository = pushRunRepository;
    this.pullRunRepository = pullRunRepository;
    this.repositoryRepository = repositoryRepository;
  }

  /**
   * Queries the database to identify all Text Unit Variants with creation or update dates newer
   * than the date specified associated with “used” Text Units for the target repo (this should
   * cover all active dev branches)
   *
   * @return The delta of text unit variants, their translations and corresponding metadata.
   */
  @Operation(summary = "Get paginated Text Unit Variants Deltas for a given set of parameters")
  @RequestMapping(value = "/api/deltas/date", method = RequestMethod.GET)
  public Page<TextUnitVariantDeltaDTO> getDeltasFromDate(
      @RequestParam(value = "repositoryId") Long repositoryId,
      @RequestParam(value = "bcp47Tags", required = false) List<String> bcp47Tags,
      // TODO(jean) what is the impact on the leniency of the WS ??
      @RequestParam(value = "fromDate", required = false) ZonedDateTime fromDate,
      @RequestParam(value = "toDate", required = false) ZonedDateTime toDate,
      @ParameterObject @PageableDefault(sort = "id", direction = Sort.Direction.DESC)
          Pageable pageable)
      throws RepositoryWithIdNotFoundException {

    Repository repository =
        repositoryRepository
            .findById(repositoryId)
            .orElseThrow(() -> new RepositoryWithIdNotFoundException(repositoryId));

    List<Locale> locales = Collections.emptyList();
    if (bcp47Tags != null && bcp47Tags.size() > 0) {
      locales = localeRepository.findAll(where(ifParamNotNull(bcp47TagIn(bcp47Tags))));
    }

    return new PageView<>(
        deltaService.getDeltasForDates(repository, locales, fromDate, toDate, pageable));
  }

  @Timed("DeltasWs.getDeltasForRuns")
  @Operation(summary = "Get Delta Content for a given set of parameters")
  @RequestMapping(value = "/api/deltas/state", method = RequestMethod.GET)
  public DeltaResponseDTO getDeltasForRuns(
      @RequestParam(value = "repositoryId") Long repositoryId,
      @RequestParam(value = "pushRunIds") List<Long> pushRunIds,
      @RequestParam(value = "bcp47Tags", required = false) List<String> bcp47Tags,
      @RequestParam(value = "pullRunIds", required = false) List<Long> pullRunIds)
      throws RepositoryWithIdNotFoundException, PushRunsMissingException {

    Repository repository =
        repositoryRepository
            .findById(repositoryId)
            .orElseThrow(() -> new RepositoryWithIdNotFoundException(repositoryId));

    List<Locale> locales = Collections.emptyList();
    if (bcp47Tags != null && bcp47Tags.size() > 0) {
      locales = localeRepository.findAll(where(ifParamNotNull(bcp47TagIn(bcp47Tags))));
    }

    List<PushRun> pushRuns = pushRunRepository.findAllById(pushRunIds);

    List<PullRun> pullRuns =
        pullRunIds == null ? Collections.emptyList() : pullRunRepository.findAllById(pullRunIds);

    return deltaService.getDeltasForRuns(repository, locales, pushRuns, pullRuns);
  }
}
