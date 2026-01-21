package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.review.ReviewProject;
import com.box.l10n.mojito.entity.review.ReviewProjectAcceptedVariant;
import com.box.l10n.mojito.entity.review.ReviewDecisionStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectRequest;
import com.box.l10n.mojito.entity.review.ReviewProjectRequestScreenshot;
import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectTextUnit;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;
import com.box.l10n.mojito.rest.review.ReviewProjectCreateRequest;
import com.box.l10n.mojito.rest.review.ReviewProjectDetailDTO;
import com.box.l10n.mojito.rest.review.ReviewProjectLocaleDetailDTO;
import com.box.l10n.mojito.rest.review.ReviewProjectLocaleSummaryDTO;
import com.box.l10n.mojito.rest.review.ReviewProjectRepositorySummaryDTO;
import com.box.l10n.mojito.rest.review.ReviewProjectSearchRequest;
import com.box.l10n.mojito.rest.review.ReviewProjectSummaryDTO;
import com.box.l10n.mojito.rest.review.ReviewProjectTextUnitDTO;
import com.box.l10n.mojito.security.AuditorAwareImpl;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class ReviewProjectService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReviewProjectService.class);

  private static final int DEFAULT_MAX_TEXT_UNITS = 1000;
  private static final int DEFAULT_SEARCH_LIMIT = 500;
  private static final int MAX_SEARCH_LIMIT = 10_000;

  private final ReviewProjectRepository reviewProjectRepository;
  private final ReviewProjectTextUnitRepository reviewProjectTextUnitRepository;
  private final ReviewProjectAcceptedVariantRepository reviewProjectAcceptedVariantRepository;
  private final ReviewProjectRequestRepository reviewProjectRequestRepository;
  private final ReviewProjectRequestScreenshotRepository reviewProjectScreenshotRepository;
  private final RepositoryRepository repositoryRepository;
  private final LocaleService localeService;
  private final TextUnitSearcher textUnitSearcher;
  private final TMTextUnitRepository tmTextUnitRepository;
  private final TMTextUnitVariantRepository tmTextUnitVariantRepository;
  private final TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;
  private final TMService tmService;
  private final AuditorAwareImpl auditorAware;

  @PersistenceContext private EntityManager entityManager;

  public ReviewProjectService(
      ReviewProjectRepository reviewProjectRepository,
      ReviewProjectTextUnitRepository reviewProjectTextUnitRepository,
      ReviewProjectAcceptedVariantRepository reviewProjectAcceptedVariantRepository,
      ReviewProjectRequestRepository reviewProjectRequestRepository,
      ReviewProjectRequestScreenshotRepository reviewProjectScreenshotRepository,
      RepositoryRepository repositoryRepository,
      LocaleService localeService,
      TextUnitSearcher textUnitSearcher,
      TMTextUnitRepository tmTextUnitRepository,
      TMTextUnitVariantRepository tmTextUnitVariantRepository,
      TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository,
      TMService tmService,
      AuditorAwareImpl auditorAware) {
    this.reviewProjectRepository = reviewProjectRepository;
    this.reviewProjectTextUnitRepository = reviewProjectTextUnitRepository;
    this.reviewProjectAcceptedVariantRepository = reviewProjectAcceptedVariantRepository;
    this.reviewProjectRequestRepository = reviewProjectRequestRepository;
    this.reviewProjectScreenshotRepository = reviewProjectScreenshotRepository;
    this.repositoryRepository = repositoryRepository;
    this.localeService = localeService;
    this.textUnitSearcher = textUnitSearcher;
    this.tmTextUnitRepository = tmTextUnitRepository;
    this.tmTextUnitVariantRepository = tmTextUnitVariantRepository;
    this.tmTextUnitCurrentVariantRepository = tmTextUnitCurrentVariantRepository;
    this.tmService = tmService;
    this.auditorAware = auditorAware;
  }

  @Transactional
  public List<ReviewProjectSummaryDTO> createReviewProject(ReviewProjectCreateRequest request) {
    if (CollectionUtils.isEmpty(request.getRepositoryIds())) {
      throw new IllegalArgumentException("At least one repository must be provided");
    }

    if (CollectionUtils.isEmpty(request.getLocaleTags())) {
      throw new IllegalArgumentException("At least one locale must be provided");
    }

    List<Repository> repositories = repositoryRepository.findAllById(request.getRepositoryIds());
    if (repositories.size() != request.getRepositoryIds().size()) {
      throw new IllegalArgumentException("One or more repositories could not be found");
    }

    if (request.getMaxTextUnits() != null && request.getMaxTextUnits() <= 0) {
      throw new IllegalArgumentException("Max text units must be greater than zero");
    }

    if (request.getMaxWordCount() != null && request.getMaxWordCount() <= 0) {
      throw new IllegalArgumentException("Max word count must be greater than zero");
    }

    if (request.getDueDate() == null) {
      throw new IllegalArgumentException("Due date must be provided");
    }

    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw new IllegalArgumentException("Name must be provided");
    }

    if (!CollectionUtils.isEmpty(request.getScreenshotImageIds())) {
      boolean anyBlank =
          request.getScreenshotImageIds().stream()
              .anyMatch(id -> id == null || id.trim().isEmpty());
      if (anyBlank) {
        throw new IllegalArgumentException("Screenshot image IDs must not be blank");
      }
    }

    Set<Repository> repositorySet = new HashSet<>(repositories);
    List<Long> repositoryIds =
        repositories.stream().map(Repository::getId).distinct().collect(Collectors.toList());

    ReviewProjectRequest reviewProjectRequest = new ReviewProjectRequest();
    reviewProjectRequest.setRequestUuid(UUID.randomUUID().toString());
    reviewProjectRequest.setName(request.getName());
    reviewProjectRequest.setNotes(request.getNotes());
    reviewProjectRequest = reviewProjectRequestRepository.save(reviewProjectRequest);

    if (!CollectionUtils.isEmpty(request.getScreenshotImageIds())) {
      saveScreenshotsForRequest(reviewProjectRequest, request.getScreenshotImageIds());
    }

    ReviewProjectType type =
        request.getType() != null ? request.getType() : ReviewProjectType.UNKNOWN;

    List<ReviewProjectSummaryDTO> summaries = new ArrayList<>();

    for (String localeTag : request.getLocaleTags()) {
      Locale locale = localeService.findByBcp47Tag(localeTag);
      if (locale == null) {
        throw new IllegalArgumentException("Unknown locale: " + localeTag);
      }

      ReviewProject reviewProject = new ReviewProject();
      reviewProject.setType(type);
      reviewProject.setStatus(ReviewProjectStatus.OPEN);
      reviewProject.setDueDate(request.getDueDate());
      reviewProject.setLocale(locale);
      reviewProject.setReviewProjectRequest(reviewProjectRequest);

      ReviewProject saved = reviewProjectRepository.save(reviewProject);

      List<TextUnitDTO> candidates =
          searchReviewCandidates(
              repositoryIds, localeTag, request.getMaxTextUnits(), request.getTmTextUnitIds());

      SelectionStats selectionStats =
          populateProjectWithTextUnits(saved, candidates, request.getMaxWordCount());

      int selectedCount = selectionStats.textUnitCount();
      if (selectedCount == 0) {
        reviewProjectRepository.delete(saved);
        continue;
      }

      saved.setTextUnitCount(selectedCount);
      saved.setWordCount(selectionStats.wordCount());
      reviewProjectRepository.save(saved);

      summaries.add(toSummaryDTO(saved, selectedCount, selectionStats.wordCount(), 0L));
    }

    if (summaries.isEmpty()) {
      reviewProjectScreenshotRepository.deleteByReviewProjectRequestId(
          reviewProjectRequest.getId());
      reviewProjectRequestRepository.delete(reviewProjectRequest);
      throw new IllegalArgumentException(
          "No text units requiring review were found for the provided selection");
    }

    return summaries;
  }

  @Transactional(readOnly = true)
  public List<ReviewProjectSummaryDTO> getOpenProjects() {
    ReviewProjectSearchRequest searchRequest = new ReviewProjectSearchRequest();
    searchRequest.setStatuses(Collections.singletonList(ReviewProjectStatus.OPEN));
    searchRequest.setLimit(MAX_SEARCH_LIMIT);
    return searchProjects(searchRequest);
  }

  @Transactional(readOnly = true)
  public List<ReviewProjectSummaryDTO> searchProjects(ReviewProjectSearchRequest request) {
    ReviewProjectSearchRequest.SearchField searchField =
        request != null && request.getSearchField() != null
            ? request.getSearchField()
            : ReviewProjectSearchRequest.SearchField.NAME;

    ReviewProjectSearchRequest.SearchMatchType searchMatchType =
        request != null && request.getSearchMatchType() != null
            ? request.getSearchMatchType()
            : ReviewProjectSearchRequest.SearchMatchType.CONTAINS;

    List<ReviewProjectStatus> statuses =
        request != null && !CollectionUtils.isEmpty(request.getStatuses())
            ? request.getStatuses()
            : List.of(ReviewProjectStatus.OPEN, ReviewProjectStatus.CLOSED);

    List<ReviewProjectType> types =
        request != null && !CollectionUtils.isEmpty(request.getTypes())
            ? request.getTypes()
            : Collections.emptyList();

    List<String> localeTags =
        request != null && !CollectionUtils.isEmpty(request.getLocaleTags())
            ? request.getLocaleTags().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(String::toLowerCase)
                .distinct()
                .toList()
            : Collections.emptyList();

    ZonedDateTime createdAfter = request != null ? request.getCreatedAfter() : null;
    ZonedDateTime createdBefore = request != null ? request.getCreatedBefore() : null;
    ZonedDateTime dueAfter = request != null ? request.getDueAfter() : null;
    ZonedDateTime dueBefore = request != null ? request.getDueBefore() : null;

    Integer limit =
        request != null && request.getLimit() != null && request.getLimit() > 0
            ? Math.min(request.getLimit(), MAX_SEARCH_LIMIT)
            : DEFAULT_SEARCH_LIMIT;

    String searchQuery =
        request != null && StringUtils.hasText(request.getSearchQuery())
            ? request.getSearchQuery().trim()
            : null;

    Long searchId = null;
    if (searchField == ReviewProjectSearchRequest.SearchField.ID && searchQuery != null) {
      try {
        searchId = Long.parseLong(searchQuery.replace("#", ""));
      } catch (NumberFormatException nfe) {
        return Collections.emptyList();
      }
    }

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<ReviewProject> cq = cb.createQuery(ReviewProject.class);
    Root<ReviewProject> root = cq.from(ReviewProject.class);
    root.fetch("locale", JoinType.LEFT);
    root.fetch("reviewProjectRequest", JoinType.LEFT);
    Join<ReviewProject, ReviewProjectRequest> requestJoin =
        root.join("reviewProjectRequest", JoinType.LEFT);

    List<Predicate> predicates = new ArrayList<>();

    if (!statuses.isEmpty()) {
      predicates.add(root.get("status").in(statuses));
    }

    if (!types.isEmpty()) {
      predicates.add(root.get("type").in(types));
    }

    if (!localeTags.isEmpty()) {
      Join<ReviewProject, Locale> localeJoin = root.join("locale", JoinType.INNER);
      Expression<String> localeTagExpression = cb.lower(localeJoin.get("bcp47Tag"));
      predicates.add(localeTagExpression.in(localeTags));
    }

    if (createdAfter != null) {
      predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), createdAfter));
    }
    if (createdBefore != null) {
      predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), createdBefore));
    }
    if (dueAfter != null) {
      predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), dueAfter));
    }
    if (dueBefore != null) {
      predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueBefore));
    }

    if (searchQuery != null) {
      if (searchField == ReviewProjectSearchRequest.SearchField.ID) {
        predicates.add(cb.equal(root.get("id"), searchId));
      } else {
        Expression<String> nameExpression = cb.lower(requestJoin.get("name"));
        String lowered = searchQuery.toLowerCase();
        Predicate searchPredicate;
        if (searchMatchType == ReviewProjectSearchRequest.SearchMatchType.EXACT) {
          searchPredicate = cb.equal(nameExpression, lowered);
        } else {
          String pattern =
              searchMatchType == ReviewProjectSearchRequest.SearchMatchType.ILIKE
                  ? "%" + lowered.replace("*", "%") + "%"
                  : "%" + lowered + "%";
          searchPredicate = cb.like(nameExpression, pattern);
        }
        predicates.add(searchPredicate);
      }
    }

    cq.select(root)
        .where(predicates.toArray(new Predicate[0]))
        .distinct(true)
        .orderBy(cb.desc(root.get("createdDate")), cb.desc(root.get("id")));

    TypedQuery<ReviewProject> query = entityManager.createQuery(cq);
    if (limit != null && limit > 0) {
      query.setMaxResults(limit);
    }

    List<ReviewProject> projects = query.getResultList();

    return projects.stream()
        .map(
            project -> {
              int totalSelected = resolveTotalSelected(project);
              int wordCount = project.getWordCount() != null ? project.getWordCount() : 0;
              long acceptedCount =
                  reviewProjectAcceptedVariantRepository.countByReviewProjectId(project.getId());
              return toSummaryDTO(project, totalSelected, wordCount, acceptedCount);
            })
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public ReviewProjectDetailDTO getProjectDetail(Long projectId)
      throws EntityWithIdNotFoundException {
    ReviewProject project =
        reviewProjectRepository
            .findById(projectId)
            .orElseThrow(() -> new EntityWithIdNotFoundException("reviewProject", projectId));

    ReviewProjectDetailDTO dto = new ReviewProjectDetailDTO();
    dto.setId(project.getId());
    dto.setType(project.getType());
    dto.setStatus(project.getStatus());
    dto.setCreatedDate(project.getCreatedDate());
    dto.setName(resolveRequestName(project));
    dto.setRequestName(resolveRequestName(project));
    dto.setDueDate(project.getDueDate());
    dto.setCloseReason(project.getCloseReason());
    dto.setTextUnitCount(project.getTextUnitCount());
    dto.setWordCount(project.getWordCount());
    dto.setNotes(resolveRequestNotes(project));
    if (project.getReviewProjectRequest() != null) {
      dto.setRequestId(project.getReviewProjectRequest().getId());
      dto.setRequestUuid(project.getReviewProjectRequest().getRequestUuid());
    }
    dto.setScreenshotImageIds(resolveScreenshotImageKeys(project));
    List<ReviewProjectTextUnitDTO> textUnits = toTextUnitDTOs(project);
    long acceptedCount = reviewProjectAcceptedVariantRepository.countByReviewProjectId(projectId);
    ReviewProjectLocaleDetailDTO localeDetail =
        new ReviewProjectLocaleDetailDTO(
            project.getId(),
            project.getLocale().getBcp47Tag(),
            project.getLocale().getBcp47Tag(),
            textUnits.size(),
            acceptedCount,
            textUnits);
    dto.setLocale(localeDetail);
    dto.setLocales(java.util.Collections.singletonList(localeDetail));
    return dto;
  }

  @Transactional
  public ReviewProjectTextUnitDTO acceptTextUnit(
      Long projectId,
      Long reviewProjectTextUnitId,
      String target,
      Boolean includedInLocalizedFile,
      Long expectedCurrentTmTextUnitVariantId,
      boolean overrideChangedCurrent,
      String reviewNotes)
      throws EntityWithIdNotFoundException {

    ReviewProject project =
        reviewProjectRepository
            .findById(projectId)
            .orElseThrow(() -> new EntityWithIdNotFoundException("reviewProject", projectId));

    ReviewProjectTextUnit textUnit =
        reviewProjectTextUnitRepository
            .findById(reviewProjectTextUnitId)
            .orElseThrow(
                () ->
                    new EntityWithIdNotFoundException(
                        "reviewProjectTextUnit", reviewProjectTextUnitId));

    if (!textUnit.getReviewProject().getId().equals(projectId)) {
      throw new IllegalArgumentException("Review project text unit does not belong to project");
    }

    if (target == null) {
      throw new IllegalArgumentException("Target translation is required");
    }

    String normalizedTarget = NormalizationUtils.normalize(target);
    boolean includeInLocalizedFile =
        includedInLocalizedFile == null ? true : includedInLocalizedFile.booleanValue();

    TMTextUnit tmTextUnit = textUnit.getTmTextUnit();
    if (tmTextUnit == null && textUnit.getTmTextUnitVariant() != null) {
      tmTextUnit = textUnit.getTmTextUnitVariant().getTmTextUnit();
    }
    if (tmTextUnit == null) {
      throw new IllegalStateException("Review project text unit missing TM reference");
    }

    TMTextUnitCurrentVariant currentVariant =
        tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(
            project.getLocale().getId(), tmTextUnit.getId());
    String currentContent =
        currentVariant != null && currentVariant.getTmTextUnitVariant() != null
            ? currentVariant.getTmTextUnitVariant().getContent()
            : null;
    Long currentVariantId =
        currentVariant != null && currentVariant.getTmTextUnitVariant() != null
            ? currentVariant.getTmTextUnitVariant().getId()
            : null;

    if (!overrideChangedCurrent
        && expectedCurrentTmTextUnitVariantId != null
        && !expectedCurrentTmTextUnitVariantId.equals(currentVariantId)) {
      TMTextUnitVariant conflictVariant =
          currentVariant != null ? currentVariant.getTmTextUnitVariant() : null;
      throw new ReviewProjectCurrentVariantConflictException(
          expectedCurrentTmTextUnitVariantId,
          currentVariantId,
          toTextUnitDTO(textUnit, conflictVariant));
    }

    TMTextUnitCurrentVariant updatedCurrentVariant =
        tmService.addTMTextUnitCurrentVariant(
            tmTextUnit.getId(),
            project.getLocale().getId(),
            normalizedTarget,
            null,
            TMTextUnitVariant.Status.APPROVED,
            includeInLocalizedFile);

    TMTextUnitVariant newVariant = updatedCurrentVariant.getTmTextUnitVariant();
    boolean changed =
        currentContent == null || !NormalizationUtils.normalize(currentContent).equals(normalizedTarget);

    ReviewProjectAcceptedVariant acceptedVariant =
        reviewProjectAcceptedVariantRepository
            .findByReviewProjectTextUnitId(textUnit.getId())
            .orElseGet(
                () -> {
                  ReviewProjectAcceptedVariant entity = new ReviewProjectAcceptedVariant();
                  entity.setReviewProject(project);
                  entity.setReviewProjectTextUnit(textUnit);
                  return entity;
                });

    acceptedVariant.setTmTextUnitVariant(
        textUnit.getTmTextUnitVariant() != null ? textUnit.getTmTextUnitVariant() : newVariant);
    acceptedVariant.setAcceptedVariant(newVariant);
    acceptedVariant.setAcceptedAt(ZonedDateTime.now());
    acceptedVariant.setAcceptedBy(auditorAware.getCurrentAuditor().orElse(null));
    reviewProjectAcceptedVariantRepository.save(acceptedVariant);

    textUnit.setReviewStatus(
        changed ? ReviewDecisionStatus.ACCEPTED_WITH_CHANGE : ReviewDecisionStatus.ACCEPTED_AS_IS);
    textUnit.setReviewTarget(changed ? normalizedTarget : null);
    textUnit.setReviewNotes(reviewNotes);
    textUnit.setReviewedAt(ZonedDateTime.now());
    textUnit.setReviewedBy(
        auditorAware.getCurrentAuditor().map(User::getUsername).orElse(null));
    reviewProjectTextUnitRepository.save(textUnit);

    return toTextUnitDTO(textUnit, newVariant);
  }

  @Transactional
  public ReviewProjectTextUnitDTO updateReviewStatus(
      Long projectId,
      Long reviewProjectTextUnitId,
      ReviewDecisionStatus reviewStatus,
      String reviewTarget,
      String reviewNotes)
      throws EntityWithIdNotFoundException {

    if (reviewStatus == null) {
      throw new IllegalArgumentException("reviewStatus is required");
    }

    ReviewProject project =
        reviewProjectRepository
            .findById(projectId)
            .orElseThrow(() -> new EntityWithIdNotFoundException("reviewProject", projectId));

    ReviewProjectTextUnit textUnit =
        reviewProjectTextUnitRepository
            .findById(reviewProjectTextUnitId)
            .orElseThrow(
                () ->
                    new EntityWithIdNotFoundException(
                        "reviewProjectTextUnit", reviewProjectTextUnitId));

    if (!textUnit.getReviewProject().getId().equals(project.getId())) {
      throw new IllegalArgumentException("Review project text unit does not belong to project");
    }

    textUnit.setReviewStatus(reviewStatus);
    textUnit.setReviewTarget(reviewTarget);
    textUnit.setReviewNotes(reviewNotes);
    textUnit.setReviewedAt(ZonedDateTime.now());
    textUnit.setReviewedBy(
        auditorAware.getCurrentAuditor().map(User::getUsername).orElse(null));
    reviewProjectTextUnitRepository.save(textUnit);

    return toTextUnitDTO(textUnit, null);
  }

  private void saveScreenshotsForRequest(
      ReviewProjectRequest reviewProjectRequest, List<String> imageKeys) {
    if (CollectionUtils.isEmpty(imageKeys)) {
      return;
    }

    Set<String> dedupedKeys =
        imageKeys.stream()
            .filter(key -> key != null && !key.trim().isEmpty())
            .map(String::trim)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    for (String imageKey : dedupedKeys) {
      ReviewProjectRequestScreenshot screenshot = new ReviewProjectRequestScreenshot();
      screenshot.setReviewProjectRequest(reviewProjectRequest);
      screenshot.setImageKey(imageKey);
      reviewProjectScreenshotRepository.save(screenshot);
    }
  }

  private List<TextUnitDTO> searchReviewCandidates(
      List<Long> repositoryIds,
      String localeTag,
      Integer configuredMaxCount,
      List<Long> tmTextUnitIds) {
    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setRepositoryIds(repositoryIds);
    params.setLocaleTags(Collections.singletonList(localeTag));
    if (tmTextUnitIds != null && !tmTextUnitIds.isEmpty()) {
      params.setTmTextUnitIds(tmTextUnitIds);
    } else {
      params.setStatusFilter(StatusFilter.REVIEW_NEEDED);
    }
    params.setPluralFormsFiltered(false);
    params.setOffset(0);
    params.setLimit(
        configuredMaxCount != null && configuredMaxCount > 0
            ? configuredMaxCount
            : tmTextUnitIds != null && !tmTextUnitIds.isEmpty()
                ? tmTextUnitIds.size()
                : DEFAULT_MAX_TEXT_UNITS);

    return textUnitSearcher.search(params);
  }

  private SelectionStats populateProjectWithTextUnits(
      ReviewProject reviewProject, List<TextUnitDTO> candidates, Integer maxWordCount) {

    if (candidates.isEmpty()) {
      return new SelectionStats(0, 0);
    }

    int position = 0;
    int accumulatedWords = 0;
    int selectedCount = 0;
    List<Long> tmTextUnitIds =
        candidates.stream().map(TextUnitDTO::getTmTextUnitId).collect(Collectors.toList());
    Map<Long, TMTextUnit> tmTextUnitMap =
        tmTextUnitRepository.findByIdIn(tmTextUnitIds).stream()
            .collect(Collectors.toMap(TMTextUnit::getId, tm -> tm));

    for (TextUnitDTO candidate : candidates) {
      Long variantId = candidate.getTmTextUnitVariantId();
      if (variantId == null) {
        continue;
      }

      TMTextUnitVariant variant = tmTextUnitVariantRepository.findById(variantId).orElse(null);
      if (variant == null) {
        continue;
      }

      TMTextUnit tmTextUnit = tmTextUnitMap.get(candidate.getTmTextUnitId());
      if (tmTextUnit == null) {
        tmTextUnit = variant.getTmTextUnit();
      }

      Integer wordCount = tmTextUnit != null ? tmTextUnit.getWordCount() : null;
      int value = wordCount != null ? wordCount : 0;
      if (maxWordCount != null && maxWordCount > 0 && accumulatedWords + value > maxWordCount) {
        break;
      }
      accumulatedWords += value;

      ReviewProjectTextUnit reviewProjectTextUnit = new ReviewProjectTextUnit();
      reviewProjectTextUnit.setReviewProject(reviewProject);
      reviewProjectTextUnit.setTmTextUnitVariant(variant);
      reviewProjectTextUnit.setTmTextUnit(
          tmTextUnit != null ? tmTextUnit : variant.getTmTextUnit());
      reviewProjectTextUnit.setPosition(position++);
      reviewProjectTextUnit.setBaselineStatus(variant.getStatus());

      reviewProjectTextUnitRepository.save(reviewProjectTextUnit);
      selectedCount++;
    }

    return new SelectionStats(selectedCount, accumulatedWords);
  }

  private int resolveTotalSelected(ReviewProject project) {
    if (project.getTextUnitCount() != null) {
      return project.getTextUnitCount();
    }
    if (project.getId() == null) {
      return 0;
    }
    return (int) reviewProjectTextUnitRepository.countByReviewProjectId(project.getId());
  }

  private List<String> resolveScreenshotImageKeys(ReviewProject project) {
    if (project.getReviewProjectRequest() == null
        || project.getReviewProjectRequest().getId() == null) {
      return Collections.emptyList();
    }
    return new ArrayList<>(
        new LinkedHashSet<>(
            reviewProjectScreenshotRepository.findImageKeysByReviewProjectRequestId(
                project.getReviewProjectRequest().getId())));
  }

  private ReviewProjectSummaryDTO toSummaryDTO(
      ReviewProject project, int totalSelected, int wordCount, long acceptedCount) {
    ReviewProjectSummaryDTO dto = new ReviewProjectSummaryDTO();
    dto.setId(project.getId());
    dto.setCreatedDate(project.getCreatedDate());
    dto.setDueDate(project.getDueDate());
    dto.setCloseReason(project.getCloseReason());
    dto.setTextUnitCount(totalSelected);
    dto.setWordCount(wordCount);
    dto.setType(project.getType());
    dto.setName(resolveRequestName(project));
    dto.setRequestName(resolveRequestName(project));
    dto.setStatus(project.getStatus());
    dto.setTotalSelected(totalSelected);
    dto.setAcceptedCount(acceptedCount);

    dto.setRepositories(Collections.emptyList());

    ReviewProjectLocaleSummaryDTO localeSummary =
        new ReviewProjectLocaleSummaryDTO(
            project.getId(),
            project.getLocale().getBcp47Tag(),
            project.getLocale().getBcp47Tag(),
            totalSelected,
            acceptedCount);
    dto.setLocales(java.util.Collections.singletonList(localeSummary));
    if (project.getReviewProjectRequest() != null) {
      dto.setRequestId(project.getReviewProjectRequest().getId());
      dto.setRequestUuid(project.getReviewProjectRequest().getRequestUuid());
    }
    dto.setScreenshotImageIds(resolveScreenshotImageKeys(project));

    return dto;
  }

  private String resolveRequestName(ReviewProject project) {
    return project.getReviewProjectRequest() != null
        ? project.getReviewProjectRequest().getName()
        : null;
  }

  private String resolveRequestNotes(ReviewProject project) {
    return project.getReviewProjectRequest() != null
        ? project.getReviewProjectRequest().getNotes()
        : null;
  }

  private List<ReviewProjectTextUnitDTO> toTextUnitDTOs(ReviewProject reviewProject) {
    return reviewProjectTextUnitRepository
        .findByReviewProjectIdOrderByPositionAsc(reviewProject.getId())
        .stream()
        .map(textUnit -> toTextUnitDTO(textUnit, null))
        .collect(Collectors.toList());
  }

  private ReviewProjectTextUnitDTO toTextUnitDTO(
      ReviewProjectTextUnit textUnit, TMTextUnitVariant variantOverride) {
    TMTextUnitVariant selectedVariantRef = textUnit.getTmTextUnitVariant();
    TMTextUnitVariant resolvedVariant = variantOverride;

    TMTextUnit tmTextUnit = textUnit.getTmTextUnit();
    if (tmTextUnit == null && selectedVariantRef != null) {
      tmTextUnit = selectedVariantRef.getTmTextUnit();
    }

    if (resolvedVariant == null
        && tmTextUnit != null
        && textUnit.getReviewProject() != null
        && textUnit.getReviewProject().getLocale() != null) {
      TMTextUnitCurrentVariant current =
          tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(
              textUnit.getReviewProject().getLocale().getId(), tmTextUnit.getId());
      if (current != null) {
        resolvedVariant = current.getTmTextUnitVariant();
      }
    }

    if (resolvedVariant == null && selectedVariantRef != null) {
      resolvedVariant =
          tmTextUnitVariantRepository
              .findById(selectedVariantRef.getId())
              .orElse(selectedVariantRef);
    }

    ReviewProjectTextUnitDTO dto = new ReviewProjectTextUnitDTO();
    dto.setReviewProjectTextUnitId(textUnit.getId());
    if (tmTextUnit != null) {
      dto.setTmTextUnitId(tmTextUnit.getId());
      dto.setName(tmTextUnit.getName());
      dto.setSource(tmTextUnit.getContent());
      if (tmTextUnit.getAsset() != null && tmTextUnit.getAsset().getRepository() != null) {
        Repository repository = tmTextUnit.getAsset().getRepository();
        dto.setRepositoryId(repository.getId());
        dto.setRepositoryName(repository.getName());
        dto.setAssetPath(tmTextUnit.getAsset().getPath());
      }
    }

    Long variantId = resolvedVariant != null ? resolvedVariant.getId() : null;
    dto.setTmTextUnitVariantId(variantId);
    dto.setCurrentTmTextUnitVariantId(variantId);
    dto.setSelectedTmTextUnitVariantId(
        selectedVariantRef != null ? selectedVariantRef.getId() : null);
    TMTextUnitVariant.Status baselineStatus =
        textUnit.getBaselineStatus() != null
            ? textUnit.getBaselineStatus()
            : selectedVariantRef != null ? selectedVariantRef.getStatus() : null;
    dto.setBaselineStatus(baselineStatus);
    dto.setReviewStatus(
        textUnit.getReviewStatus() != null
            ? textUnit.getReviewStatus()
            : ReviewDecisionStatus.PENDING);
    dto.setReviewTarget(textUnit.getReviewTarget());
    dto.setReviewNotes(textUnit.getReviewNotes());
    dto.setReviewedAt(textUnit.getReviewedAt());
    dto.setReviewedBy(textUnit.getReviewedBy());

    // Expose original selected translation as target (baseline) and current/accepted content separately.
    dto.setTarget(selectedVariantRef != null ? selectedVariantRef.getContent() : null);
    if (resolvedVariant != null) {
      dto.setCurrentTarget(resolvedVariant.getContent());
      dto.setStatus(resolvedVariant.getStatus());
      dto.setIncludedInLocalizedFile(resolvedVariant.isIncludedInLocalizedFile());
    }

    return dto;
  }

  private record SelectionStats(int textUnitCount, int wordCount) {}
}
