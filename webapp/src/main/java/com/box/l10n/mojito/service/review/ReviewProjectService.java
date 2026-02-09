package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.*;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Locale_;
import com.box.l10n.mojito.entity.review.*;
import com.box.l10n.mojito.entity.review.ReviewProject;
import com.box.l10n.mojito.entity.review.ReviewProjectRequest;
import com.box.l10n.mojito.entity.review.ReviewProjectRequest_;
import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectTextUnitDecision.DecisionState;
// TODO(JA) NO rest !!
import com.box.l10n.mojito.entity.review.ReviewProject_;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.WordCountService;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.security.user.UserService;
import com.box.l10n.mojito.service.tm.AddTMTextUnitCurrentVariantResult;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class ReviewProjectService {

  private static final Logger logger = LoggerFactory.getLogger(ReviewProjectService.class);

  private final ReviewProjectRepository reviewProjectRepository;
  private final ReviewProjectTextUnitRepository reviewProjectTextUnitRepository;
  private final ReviewProjectTextUnitDecisionRepository reviewProjectTextUnitDecisionRepository;
  private final ReviewProjectRequestRepository reviewProjectRequestRepository;
  private final ReviewProjectRequestScreenshotRepository reviewProjectScreenshotRepository;
  private final LocaleService localeService;
  private final TextUnitSearcher textUnitSearcher;
  private final TMTextUnitRepository tmTextUnitRepository;
  private final TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;
  private final TMService tmService;
  private final WordCountService wordCountService;
  private final UserService userService;

  @PersistenceContext private EntityManager entityManager;

  public ReviewProjectService(
      ReviewProjectRepository reviewProjectRepository,
      ReviewProjectTextUnitRepository reviewProjectTextUnitRepository,
      ReviewProjectTextUnitDecisionRepository reviewProjectTextUnitDecisionRepository,
      ReviewProjectRequestRepository reviewProjectRequestRepository,
      ReviewProjectRequestScreenshotRepository reviewProjectScreenshotRepository,
      LocaleService localeService,
      TextUnitSearcher textUnitSearcher,
      TMTextUnitRepository tmTextUnitRepository,
      TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository,
      TMService tmService,
      WordCountService wordCountService,
      UserService userService) {
    this.reviewProjectRepository = reviewProjectRepository;
    this.reviewProjectTextUnitRepository = reviewProjectTextUnitRepository;
    this.reviewProjectTextUnitDecisionRepository = reviewProjectTextUnitDecisionRepository;
    this.reviewProjectRequestRepository = reviewProjectRequestRepository;
    this.reviewProjectScreenshotRepository = reviewProjectScreenshotRepository;
    this.localeService = localeService;
    this.textUnitSearcher = textUnitSearcher;
    this.tmTextUnitRepository = tmTextUnitRepository;
    this.tmTextUnitCurrentVariantRepository = tmTextUnitCurrentVariantRepository;
    this.tmService = tmService;
    this.wordCountService = wordCountService;
    this.userService = userService;
  }

  @Transactional
  public CreateReviewProjectRequestResult createReviewProjectRequest(
      CreateReviewProjectRequestCommand request) {
    if (CollectionUtils.isEmpty(request.localeTags())) {
      throw new IllegalArgumentException("At least one locale must be provided");
    }

    if (request.dueDate() == null) {
      throw new IllegalArgumentException("Due date must be provided");
    }

    if (request.name() == null || request.name().trim().isEmpty()) {
      throw new IllegalArgumentException("Name must be provided");
    }

    if (request.tmTextUnitIds() == null || request.tmTextUnitIds().isEmpty()) {
      throw new IllegalArgumentException("tmTextUnitIds must be provided");
    }

    if (request.type() == null) {
      throw new IllegalArgumentException("type must be provided");
    }

    List<LocaleCandidates> localesToCreate = new ArrayList<>();
    for (String localeTag : new LinkedHashSet<>(request.localeTags())) {
      Locale locale = localeService.findByBcp47Tag(localeTag);

      if (locale == null) {
        throw new IllegalArgumentException("Unknown locale: " + localeTag);
      }

      List<TextUnitDTO> candidates = searchReviewCandidates(request.tmTextUnitIds(), locale);
      if (candidates.isEmpty()) {
        continue;
      }

      localesToCreate.add(new LocaleCandidates(locale, candidates));
    }

    if (localesToCreate.isEmpty()) {
      throw new IllegalArgumentException("No text units found for provided locales");
    }

    ReviewProjectRequest reviewProjectRequest = new ReviewProjectRequest();
    reviewProjectRequest.setName(request.name());
    reviewProjectRequest.setNotes(request.notes());
    reviewProjectRequest = reviewProjectRequestRepository.save(reviewProjectRequest);

    if (request.screenshotImageIds() != null) {
      for (String screenshotImageId : request.screenshotImageIds()) {
        ReviewProjectRequestScreenshot screenshot = new ReviewProjectRequestScreenshot();
        screenshot.setReviewProjectRequest(reviewProjectRequest);
        screenshot.setImageName(screenshotImageId);
        reviewProjectScreenshotRepository.save(screenshot);
      }
    }

    List<Long> projectIds = new ArrayList<>();
    List<String> createdLocaleTags = new ArrayList<>();

    for (LocaleCandidates localeCandidates : localesToCreate) {
      Locale locale = localeCandidates.locale();
      ReviewProject reviewProject = new ReviewProject();
      reviewProject.setType(request.type());
      reviewProject.setStatus(ReviewProjectStatus.OPEN);
      reviewProject.setDueDate(request.dueDate());
      reviewProject.setLocale(locale);
      reviewProject.setReviewProjectRequest(reviewProjectRequest);

      ReviewProject saved = reviewProjectRepository.save(reviewProject);

      int wordCount = 0;
      int textUnitCount = 0;

      for (TextUnitDTO textUnitDTO : localeCandidates.candidates()) {
        ReviewProjectTextUnit reviewProjectTextUnit = new ReviewProjectTextUnit();
        reviewProjectTextUnit.setReviewProject(reviewProject);
        reviewProjectTextUnit.setTmTextUnit(
            entityManager.getReference(TMTextUnit.class, textUnitDTO.getTmTextUnitId()));
        if (textUnitDTO.getTmTextUnitVariantId() != null) {
          reviewProjectTextUnit.setTmTextUnitVariant(
              entityManager.getReference(
                  TMTextUnitVariant.class, textUnitDTO.getTmTextUnitVariantId()));
        }
        reviewProjectTextUnitRepository.save(reviewProjectTextUnit);

        textUnitCount++;
        wordCount += wordCountService.getEnglishWordCount(textUnitDTO.getSource());
      }

      saved.setWordCount(wordCount);
      saved.setTextUnitCount(textUnitCount);

      projectIds.add(saved.getId());
      createdLocaleTags.add(locale.getBcp47Tag());
    }

    return new CreateReviewProjectRequestResult(
        reviewProjectRequest.getId(),
        reviewProjectRequest.getName(),
        createdLocaleTags,
        request.dueDate(),
        projectIds);
  }

  private record LocaleCandidates(Locale locale, List<TextUnitDTO> candidates) {}

  @Transactional(readOnly = true)
  public SearchReviewProjectsView searchReviewProjects(SearchReviewProjectsCriteria request) {

    if (request == null) {
      throw new IllegalArgumentException("request must not be null");
    }

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<SearchReviewProjectDetail> cq = cb.createQuery(SearchReviewProjectDetail.class);
    Root<ReviewProject> root = cq.from(ReviewProject.class);
    Join<ReviewProject, Locale> localeJoin = root.join(ReviewProject_.locale, JoinType.LEFT);
    Join<ReviewProject, ReviewProjectRequest> requestJoin =
        root.join(ReviewProject_.reviewProjectRequest, JoinType.LEFT);

    List<Predicate> predicates = new ArrayList<>();

    if (request.statuses() != null) {
      predicates.add(root.get("status").in(request.statuses()));
    }

    if (request.types() != null) {
      predicates.add(root.get("type").in(request.types()));
    }

    if (request.localeTags() != null) {
      predicates.add(localeJoin.get(Locale_.bcp47Tag).in(request.localeTags()));
    }

    if (request.createdAfter() != null) {
      predicates.add(
          cb.greaterThanOrEqualTo(root.get(ReviewProject_.createdDate), request.createdAfter()));
    }
    if (request.createdBefore() != null) {
      predicates.add(
          cb.lessThanOrEqualTo(root.get(ReviewProject_.createdDate), request.createdBefore()));
    }
    if (request.dueAfter() != null) {
      predicates.add(cb.greaterThanOrEqualTo(root.get(ReviewProject_.dueDate), request.dueAfter()));
    }
    if (request.dueBefore() != null) {
      predicates.add(cb.lessThanOrEqualTo(root.get(ReviewProject_.dueDate), request.dueBefore()));
    }

    if (request.searchQuery() != null) {
      predicates.add(buildSearchPredicate(cb, root, requestJoin, request));
    }

    Predicate[] predicateArray = predicates.toArray(Predicate[]::new);

    cq.where(predicateArray)
        .select(
            cb.construct(
                SearchReviewProjectDetail.class,
                root.get(ReviewProject_.id),
                root.get(ReviewProject_.createdDate),
                root.get(ReviewProject_.lastModifiedDate),
                root.get(ReviewProject_.dueDate),
                root.get(ReviewProject_.closeReason),
                root.get(ReviewProject_.textUnitCount),
                root.get(ReviewProject_.wordCount),
                root.get(ReviewProject_.type),
                root.get(ReviewProject_.status),
                localeJoin.get(Locale_.id),
                localeJoin.get(Locale_.bcp47Tag),
                requestJoin.get(ReviewProjectRequest_.id),
                requestJoin.get(ReviewProjectRequest_.name)))
        .distinct(true)
        .orderBy(cb.desc(root.get(ReviewProject_.id)));

    TypedQuery<SearchReviewProjectDetail> query = entityManager.createQuery(cq);
    query.setMaxResults(request.limit());

    List<SearchReviewProjectDetail> projectDetails = query.getResultList();

    Map<Long, Long> acceptedCountByProjectId = new HashMap<>();
    if (!projectDetails.isEmpty()) {
      List<Long> projectIds = projectDetails.stream().map(SearchReviewProjectDetail::id).toList();
      List<ReviewProjectAcceptedCountRow> acceptedCounts =
          reviewProjectTextUnitDecisionRepository.countAcceptedByProjectIdsAndDecisionState(
              projectIds, DecisionState.DECIDED);
      for (ReviewProjectAcceptedCountRow acceptedCount : acceptedCounts) {
        acceptedCountByProjectId.put(acceptedCount.projectId(), acceptedCount.acceptedCount());
      }
    }

    List<SearchReviewProjectsView.ReviewProject> reviewProjects =
        projectDetails.stream()
            .map(
                detail -> {
                  long acceptedCount = acceptedCountByProjectId.getOrDefault(detail.id(), 0L);
                  return new SearchReviewProjectsView.ReviewProject(
                      detail.id(),
                      detail.createdDate(),
                      detail.lastModifiedDate(),
                      detail.dueDate(),
                      detail.closeReason(),
                      detail.textUnitCount(),
                      detail.wordCount(),
                      acceptedCount,
                      detail.type(),
                      detail.status(),
                      new SearchReviewProjectsView.Locale(detail.localeId(), detail.localeTag()),
                      new SearchReviewProjectsView.ReviewProjectRequest(
                          detail.requestId(), detail.requestName()));
                })
            .toList();

    return new SearchReviewProjectsView(reviewProjects);
  }

  private Predicate buildStringPredicate(
      CriteriaBuilder cb,
      Expression<String> expression,
      String searchQuery,
      SearchReviewProjectsCriteria.SearchMatchType matchType) {

    boolean ignoreCase = matchType == SearchReviewProjectsCriteria.SearchMatchType.ILIKE;
    String queryValue = ignoreCase ? searchQuery.toLowerCase() : searchQuery;
    Expression<String> valueExpression = ignoreCase ? cb.lower(expression) : expression;

    return switch (matchType) {
      case EXACT -> cb.equal(valueExpression, searchQuery);
      case ILIKE, CONTAINS -> {
        String pattern = "%" + escapeForLike(queryValue) + "%";
        yield cb.like(valueExpression, pattern, '\\');
      }
    };
  }

  private Predicate buildSearchPredicate(
      CriteriaBuilder cb,
      Root<ReviewProject> root,
      Join<ReviewProject, ReviewProjectRequest> requestJoin,
      SearchReviewProjectsCriteria request) {
    SearchReviewProjectsCriteria.SearchField searchField = request.searchField();
    SearchReviewProjectsCriteria.SearchMatchType matchType = request.searchMatchType();

    Expression<String> expression;
    expression =
        switch (searchField) {
          case ID -> root.get(ReviewProject_.id).as(String.class);
          case NAME -> requestJoin.get(ReviewProjectRequest_.name);
          case REQUEST_ID -> requestJoin.get(ReviewProjectRequest_.id).as(String.class);
        };

    return buildStringPredicate(cb, expression, request.searchQuery(), matchType);
  }

  private String escapeForLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  @Transactional(readOnly = true)
  public GetProjectDetailView getProjectDetail(Long projectId) {
    ReviewProjectDetail project =
        reviewProjectRepository
            .findDetailById(projectId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "reviewProject with id: " + projectId + " not found"));

    List<ReviewProjectTextUnitDetail> textUnitDetails =
        reviewProjectTextUnitRepository.findDetailByReviewProjectId(projectId);

    List<GetProjectDetailView.ReviewProjectTextUnit> reviewProjectTextUnits =
        textUnitDetails.stream().map(this::toReviewProjectTextUnit).toList();

    List<String> screenshotImageIds =
        project.reviewProjectRequestId() == null
            ? List.of()
            : reviewProjectScreenshotRepository.findImageNamesByReviewProjectRequestId(
                project.reviewProjectRequestId());

    return new GetProjectDetailView(
        project.id(),
        project.type(),
        project.status(),
        project.createdDate(),
        project.dueDate(),
        project.closeReason(),
        project.textUnitCount(),
        project.wordCount(),
        new GetProjectDetailView.ReviewProjectRequest(
            project.reviewProjectRequestId(),
            project.reviewProjectRequestName(),
            screenshotImageIds),
        new GetProjectDetailView.Locale(project.localeId(), project.localeTag()),
        reviewProjectTextUnits);
  }

  @Transactional
  public GetProjectDetailView updateProjectStatus(
      Long projectId, ReviewProjectStatus status, String closeReason) {
    if (status == null) {
      throw new IllegalArgumentException("status must be provided");
    }

    ReviewProject reviewProject =
        reviewProjectRepository
            .findById(projectId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "reviewProject with id: " + projectId + " not found"));

    if (!userService.isCurrentUserAdmin()) {
      userService.checkUserCanEditLocale(reviewProject.getLocale().getId());
    }

    reviewProject.setStatus(status);
    if (status == ReviewProjectStatus.OPEN) {
      reviewProject.setCloseReason(null);
    } else if (closeReason != null) {
      String trimmed = closeReason.trim();
      reviewProject.setCloseReason(trimmed.isEmpty() ? null : trimmed);
    }

    reviewProjectRepository.save(reviewProject);
    return getProjectDetail(projectId);
  }

  @Transactional
  public int adminBatchUpdateStatus(
      List<Long> projectIds, ReviewProjectStatus status, String closeReason) {
    requireAdmin();
    if (CollectionUtils.isEmpty(projectIds)) {
      return 0;
    }
    if (status == null) {
      throw new IllegalArgumentException("status must be provided");
    }

    List<Long> distinctIds = projectIds.stream().filter(Objects::nonNull).distinct().toList();
    if (distinctIds.isEmpty()) {
      return 0;
    }

    List<ReviewProject> projects = reviewProjectRepository.findAllById(distinctIds);
    for (ReviewProject project : projects) {
      project.setStatus(status);
      if (status == ReviewProjectStatus.OPEN) {
        project.setCloseReason(null);
      } else if (closeReason != null) {
        String trimmed = closeReason.trim();
        project.setCloseReason(trimmed.isEmpty() ? null : trimmed);
      }
    }

    reviewProjectRepository.saveAll(projects);
    return projects.size();
  }

  @Transactional
  public int adminBatchDeleteProjects(List<Long> projectIds) {
    requireAdmin();
    if (CollectionUtils.isEmpty(projectIds)) {
      return 0;
    }

    List<Long> distinctIds = projectIds.stream().filter(Objects::nonNull).distinct().toList();
    if (distinctIds.isEmpty()) {
      return 0;
    }

    List<Long> requestIds = reviewProjectRepository.findRequestIdsByProjectIds(distinctIds);

    reviewProjectTextUnitDecisionRepository.deleteByReviewProjectIds(distinctIds);
    reviewProjectTextUnitRepository.deleteByReviewProjectIds(distinctIds);
    int deletedProjects = reviewProjectRepository.deleteByProjectIds(distinctIds);

    if (!CollectionUtils.isEmpty(requestIds)) {
      List<Long> orphanRequestIds = reviewProjectRequestRepository.findOrphanRequestIds(requestIds);
      if (!CollectionUtils.isEmpty(orphanRequestIds)) {
        reviewProjectScreenshotRepository.deleteByReviewProjectRequestIdIn(orphanRequestIds);
        reviewProjectRequestRepository.deleteAllById(orphanRequestIds);
      }
    }

    return deletedProjects;
  }

  @Transactional
  public ReviewProjectTextUnitDetail saveDecision(
      Long reviewProjectTextUnitId,
      String target,
      String comment,
      String status,
      Boolean includedInLocalizedFile,
      DecisionState decisionState,
      Long expectedCurrentTmTextUnitVariantId,
      boolean overrideChangedCurrent,
      String decisionNotes) {

    boolean hasTarget = target != null;
    if (hasTarget) {
      if (status == null) {
        throw new IllegalArgumentException("Status is required");
      }
      if (includedInLocalizedFile == null) {
        throw new IllegalArgumentException("includedInLocalizedFile is required");
      }
    }

    if (decisionState == null) {
      throw new IllegalArgumentException("decisionState is required");
    }

    boolean requireCurrentVariantMatch = hasTarget || decisionState == DecisionState.DECIDED;

    ReviewProjectTextUnit textUnit =
        reviewProjectTextUnitRepository
            .findById(reviewProjectTextUnitId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "reviewProjectTextUnit with id: "
                            + reviewProjectTextUnitId
                            + " not found"));

    ReviewProject project = textUnit.getReviewProject();
    userService.checkUserCanEditLocale(project.getLocale().getId());

    TMTextUnitVariant baselineVariant = textUnit.getTmTextUnitVariant();
    TMTextUnit tmTextUnit = textUnit.getTmTextUnit();

    TMTextUnitCurrentVariant currentVariant =
        tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(
            project.getLocale().getId(), tmTextUnit.getId());
    TMTextUnitVariant currentTmTextUnitVariant =
        currentVariant != null ? currentVariant.getTmTextUnitVariant() : null;
    Long currentVariantId =
        currentTmTextUnitVariant != null ? currentTmTextUnitVariant.getId() : null;

    if (requireCurrentVariantMatch
        && !overrideChangedCurrent
        && expectedCurrentTmTextUnitVariantId != null
        && !expectedCurrentTmTextUnitVariantId.equals(currentVariantId)) {
      throw new ReviewProjectCurrentVariantConflictException(
          expectedCurrentTmTextUnitVariantId,
          currentVariantId,
          fetchReviewProjectTextUnitDetail(reviewProjectTextUnitId));
    }

    Optional<ReviewProjectTextUnitDecision> existingDecision =
        reviewProjectTextUnitDecisionRepository.findByReviewProjectTextUnitId(
            reviewProjectTextUnitId);
    if (!hasTarget && decisionState == DecisionState.PENDING && existingDecision.isEmpty()) {
      return fetchReviewProjectTextUnitDetail(reviewProjectTextUnitId);
    }

    ReviewProjectTextUnitDecision decision =
        existingDecision.orElseGet(
            () -> {
              ReviewProjectTextUnitDecision entity = new ReviewProjectTextUnitDecision();
              entity.setReviewProjectTextUnit(textUnit);
              return entity;
            });
    TMTextUnitVariant reviewedVariant =
        baselineVariant != null ? baselineVariant : currentTmTextUnitVariant;

    if (hasTarget) {
      String normalizedTarget = NormalizationUtils.normalize(target);
      TMTextUnitVariant.Status statusEnum = TMTextUnitVariant.Status.valueOf(status);

      AddTMTextUnitCurrentVariantResult addResult =
          tmService.addTMTextUnitCurrentVariantWithResult(
              currentVariant,
              tmTextUnit.getTm().getId(),
              tmTextUnit.getAsset().getId(),
              tmTextUnit.getId(),
              project.getLocale().getId(),
              normalizedTarget,
              comment,
              statusEnum,
              includedInLocalizedFile,
              null,
              null);
      TMTextUnitVariant decidedVariant =
          addResult.getTmTextUnitCurrentVariant().getTmTextUnitVariant();

      decision.setReviewedVariant(reviewedVariant);
      decision.setDecisionVariant(decidedVariant);
      decision.setNotes(decisionNotes);
    } else if (decisionState == DecisionState.DECIDED && decision.getDecisionVariant() == null) {
      TMTextUnitVariant fallbackVariant =
          currentTmTextUnitVariant != null ? currentTmTextUnitVariant : baselineVariant;
      decision.setDecisionVariant(fallbackVariant);
      decision.setReviewedVariant(reviewedVariant);
    }

    decision.setDecisionState(decisionState);
    reviewProjectTextUnitDecisionRepository.save(decision);
    return fetchReviewProjectTextUnitDetail(reviewProjectTextUnitId);
  }

  private ReviewProjectTextUnitDetail fetchReviewProjectTextUnitDetail(
      Long reviewProjectTextUnitId) {
    return reviewProjectTextUnitRepository
        .findDetailByReviewProjectTextUnitId(reviewProjectTextUnitId)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "reviewProjectTextUnit with id: " + reviewProjectTextUnitId + " not found"));
  }

  private GetProjectDetailView.ReviewProjectTextUnit toReviewProjectTextUnit(
      ReviewProjectTextUnitDetail detail) {
    GetProjectDetailView.Asset.Repository repository =
        new GetProjectDetailView.Asset.Repository(detail.repositoryId(), detail.repositoryName());
    GetProjectDetailView.Asset assetView =
        new GetProjectDetailView.Asset(detail.assetPath(), repository);
    GetProjectDetailView.TmTextUnit tmTextUnitView =
        new GetProjectDetailView.TmTextUnit(
            detail.tmTextUnitId(),
            detail.tmTextUnitName(),
            detail.tmTextUnitContent(),
            detail.tmTextUnitComment(),
            assetView,
            detail.tmTextUnitWordCount() != null ? detail.tmTextUnitWordCount().longValue() : null);
    GetProjectDetailView.TmTextUnitVariant baselineTmTextUnitVariantView =
        detail.baselineTmTextUnitVariantId() == null
            ? new GetProjectDetailView.TmTextUnitVariant(null, null, null, null, null)
            : new GetProjectDetailView.TmTextUnitVariant(
                detail.baselineTmTextUnitVariantId(),
                detail.baselineTmTextUnitVariantContent(),
                detail.baselineTmTextUnitVariantStatus() != null
                    ? detail.baselineTmTextUnitVariantStatus().name()
                    : null,
                detail.baselineTmTextUnitVariantIncludedInLocalizedFile(),
                detail.baselineTmTextUnitVariantComment());
    GetProjectDetailView.TmTextUnitVariant currentTmTextUnitVariantView =
        detail.currentTmTextUnitVariantId() == null
            ? new GetProjectDetailView.TmTextUnitVariant(null, null, null, null, null)
            : new GetProjectDetailView.TmTextUnitVariant(
                detail.currentTmTextUnitVariantId(),
                detail.currentTmTextUnitVariantContent(),
                detail.currentTmTextUnitVariantStatus() != null
                    ? detail.currentTmTextUnitVariantStatus().name()
                    : null,
                detail.currentTmTextUnitVariantIncludedInLocalizedFile(),
                detail.currentTmTextUnitVariantComment());
    GetProjectDetailView.TmTextUnitVariant decisionTmTextUnitVariantView =
        detail.decisionVariantId() == null
            ? null
            : new GetProjectDetailView.TmTextUnitVariant(
                detail.decisionVariantId(),
                detail.decisionVariantContent(),
                detail.decisionVariantStatus() != null
                    ? detail.decisionVariantStatus().name()
                    : null,
                detail.decisionVariantIncludedInLocalizedFile(),
                detail.decisionVariantComment());
    boolean hasDecision =
        detail.decisionState() != null
            || detail.decisionVariantId() != null
            || detail.reviewedTmTextUnitVariantId() != null
            || detail.decisionNotes() != null;
    GetProjectDetailView.ReviewProjectTextUnitDecision reviewProjectTextUnitDecision =
        hasDecision
            ? new GetProjectDetailView.ReviewProjectTextUnitDecision(
                detail.reviewedTmTextUnitVariantId(),
                detail.decisionNotes(),
                detail.decisionState(),
                decisionTmTextUnitVariantView)
            : null;
    return new GetProjectDetailView.ReviewProjectTextUnit(
        detail.reviewProjectTextUnitId(),
        tmTextUnitView,
        baselineTmTextUnitVariantView,
        currentTmTextUnitVariantView,
        reviewProjectTextUnitDecision);
  }

  private List<TextUnitDTO> searchReviewCandidates(List<Long> tmTextUnitIds, Locale locale) {
    if (tmTextUnitIds == null || tmTextUnitIds.isEmpty()) {
      throw new IllegalArgumentException("tmTextUnitIds must be provided");
    }

    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setTmTextUnitIds(tmTextUnitIds);
    params.setLocaleId(locale.getId());
    params.setPluralFormsFiltered(false);
    params.setLimit(tmTextUnitIds.size());

    return textUnitSearcher.search(params);
  }

  private void requireAdmin() {
    if (!userService.isCurrentUserAdmin()) {
      throw new AccessDeniedException("Admin role required");
    }
  }
}
