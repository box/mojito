package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.review.*;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.locale.LocaleService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;
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
  private final ReviewProjectTextUnitDecisionRepository reviewProjectTextUnitDecisionRepository;
  private final ReviewProjectRequestRepository reviewProjectRequestRepository;
  private final ReviewProjectRequestScreenshotRepository reviewProjectScreenshotRepository;
  private final LocaleService localeService;
  private final TextUnitSearcher textUnitSearcher;
  private final TMTextUnitRepository tmTextUnitRepository;
  private final TMTextUnitVariantRepository tmTextUnitVariantRepository;
  private final TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;
  private final TMService tmService;

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
      TMTextUnitVariantRepository tmTextUnitVariantRepository,
      TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository,
      TMService tmService) {
    this.reviewProjectRepository = reviewProjectRepository;
    this.reviewProjectTextUnitRepository = reviewProjectTextUnitRepository;
    this.reviewProjectTextUnitDecisionRepository = reviewProjectTextUnitDecisionRepository;
    this.reviewProjectRequestRepository = reviewProjectRequestRepository;
    this.reviewProjectScreenshotRepository = reviewProjectScreenshotRepository;
    this.localeService = localeService;
    this.textUnitSearcher = textUnitSearcher;
    this.tmTextUnitRepository = tmTextUnitRepository;
    this.tmTextUnitVariantRepository = tmTextUnitVariantRepository;
    this.tmTextUnitCurrentVariantRepository = tmTextUnitCurrentVariantRepository;
    this.tmService = tmService;
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

    ReviewProjectRequest reviewProjectRequest = new ReviewProjectRequest();
    reviewProjectRequest.setName(request.name());
    reviewProjectRequest.setNotes(request.notes());
    reviewProjectRequest = reviewProjectRequestRepository.save(reviewProjectRequest);

    if (!CollectionUtils.isEmpty(request.screenshotImageIds())) {
      saveScreenshotsForRequest(reviewProjectRequest, request.screenshotImageIds());
    }

    ReviewProjectType type = request.type() != null ? request.type() : ReviewProjectType.UNKNOWN;

    List<Long> projectIds = new ArrayList<>();
    List<TextUnitDTO> candidates = searchReviewCandidates(request.tmTextUnitIds());

    for (String localeTag : request.localeTags()) {
      Locale locale = localeService.findByBcp47Tag(localeTag);
      if (locale == null) {
        throw new IllegalArgumentException("Unknown locale: " + localeTag);
      }

      ReviewProject reviewProject = new ReviewProject();
      reviewProject.setType(type);
      reviewProject.setStatus(ReviewProjectStatus.OPEN);
      reviewProject.setDueDate(request.dueDate());
      reviewProject.setLocale(locale);
      reviewProject.setReviewProjectRequest(reviewProjectRequest);

      ReviewProject saved = reviewProjectRepository.save(reviewProject);

      populateProjectWithTextUnits(saved, candidates);
      reviewProjectRepository.save(saved);

      projectIds.add(saved.getId());
    }

    return new CreateReviewProjectRequestResult(
        reviewProjectRequest.getId(),
        reviewProjectRequest.getName(),
        request.localeTags(),
        request.dueDate(),
        projectIds);
  }

  @Transactional(readOnly = true)
  public SearchReviewProjectsView searchReviewProjects(SearchReviewProjectsCriteria request) {
    SearchReviewProjectsCriteria.SearchField searchField =
        request != null && request.getSearchField() != null
            ? request.getSearchField()
            : SearchReviewProjectsCriteria.SearchField.NAME;

    SearchReviewProjectsCriteria.SearchMatchType searchMatchType =
        request != null && request.getSearchMatchType() != null
            ? request.getSearchMatchType()
            : SearchReviewProjectsCriteria.SearchMatchType.CONTAINS;

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
    if (searchField == SearchReviewProjectsCriteria.SearchField.ID && searchQuery != null) {
      try {
        searchId = Long.parseLong(searchQuery.replace("#", ""));
      } catch (NumberFormatException nfe) {
        return new SearchReviewProjectsView(List.of());
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
      if (searchField == SearchReviewProjectsCriteria.SearchField.ID) {
        predicates.add(cb.equal(root.get("id"), searchId));
      } else {
        Expression<String> nameExpression = cb.lower(requestJoin.get("name"));
        String lowered = searchQuery.toLowerCase();
        Predicate searchPredicate;
        if (searchMatchType == SearchReviewProjectsCriteria.SearchMatchType.EXACT) {
          searchPredicate = cb.equal(nameExpression, lowered);
        } else {
          String pattern =
              searchMatchType == SearchReviewProjectsCriteria.SearchMatchType.ILIKE
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

    List<SearchReviewProjectsView.ReviewProject> summaries =
        projects.stream()
            .map(
                project -> {
                  int totalSelected = resolveTotalSelected(project);
                  int wordCount = project.getWordCount() != null ? project.getWordCount() : 0;
                  return toSummaryView(project, totalSelected, wordCount);
                })
            .toList();

    return new SearchReviewProjectsView(summaries);
  }

  @Transactional(readOnly = true)
  public ReviewProjectDetail getProjectDetail(Long projectId) throws EntityWithIdNotFoundException {
    ReviewProject project =
        reviewProjectRepository
            .findById(projectId)
            .orElseThrow(() -> new EntityWithIdNotFoundException("reviewProject", projectId));

    List<ReviewProjectTextUnitView> textUnitViews = toTextUnitViews(project);
    List<ReviewProjectDetail.ReviewProjectTextUnit> textUnits =
        textUnitViews.stream().map(this::toDetailTextUnit).toList();

    ReviewProjectDetail.ReviewProjectRequest request =
        project.getReviewProjectRequest() != null
            ? new ReviewProjectDetail.ReviewProjectRequest(
                project.getReviewProjectRequest().getId(),
                project.getReviewProjectRequest().getName(),
                resolveScreenshotImageNames(project))
            : null;

    ReviewProjectDetail.Locale locale =
        project.getLocale() != null
            ? new ReviewProjectDetail.Locale(
                project.getLocale().getId(), project.getLocale().getBcp47Tag())
            : null;

    return new ReviewProjectDetail(
        project.getId(),
        project.getType(),
        project.getStatus(),
        project.getCreatedDate(),
        project.getDueDate(),
        project.getCloseReason(),
        project.getTextUnitCount(),
        project.getWordCount(),
        request,
        locale,
        textUnits);
  }

  @Transactional
  public ReviewProjectDetail.ReviewProjectTextUnit acceptTextUnit(
      Long projectId,
      Long reviewProjectTextUnitId,
      String target,
      Boolean includedInLocalizedFile,
      Long expectedCurrentTmTextUnitVariantId,
      boolean overrideChangedCurrent,
      String notes)
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
      ReviewProjectTextUnitDecision decision =
          reviewProjectTextUnitDecisionRepository
              .findByReviewProjectTextUnitId(textUnit.getId())
              .orElse(null);
      throw new ReviewProjectCurrentVariantConflictException(
          expectedCurrentTmTextUnitVariantId,
          currentVariantId,
          toDetailTextUnit(textUnit, decision));
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
        currentContent == null
            || !NormalizationUtils.normalize(currentContent).equals(normalizedTarget);

    ReviewProjectTextUnitDecision variantDecision =
        reviewProjectTextUnitDecisionRepository
            .findByReviewProjectTextUnitId(textUnit.getId())
            .orElseGet(
                () -> {
                  ReviewProjectTextUnitDecision entity = new ReviewProjectTextUnitDecision();
                  entity.setReviewProjectTextUnit(textUnit);
                  return entity;
                });

    variantDecision.setVariant(newVariant);
    variantDecision.setNotes(notes);
    reviewProjectTextUnitDecisionRepository.save(variantDecision);

    return toDetailTextUnit(textUnit, variantDecision);
  }

  @Transactional
  public ReviewProjectDetail.ReviewProjectTextUnit updateReviewStatus(
      Long projectId, Long reviewProjectTextUnitId, String notes)
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

    if (!textUnit.getReviewProject().getId().equals(project.getId())) {
      throw new IllegalArgumentException("Review project text unit does not belong to project");
    }

    ReviewProjectTextUnitDecision decision =
        reviewProjectTextUnitDecisionRepository
            .findByReviewProjectTextUnitId(reviewProjectTextUnitId)
            .orElseGet(
                () -> {
                  ReviewProjectTextUnitDecision entity = new ReviewProjectTextUnitDecision();
                  entity.setReviewProjectTextUnit(textUnit);
                  return entity;
                });

    decision.setNotes(notes);
    reviewProjectTextUnitDecisionRepository.save(decision);

    return toDetailTextUnit(textUnit, decision);
  }

  private void saveScreenshotsForRequest(
      ReviewProjectRequest reviewProjectRequest, List<String> imageNames) {
    if (CollectionUtils.isEmpty(imageNames)) {
      return;
    }

    Set<String> dedupedNames =
        imageNames.stream()
            .filter(name -> name != null && !name.trim().isEmpty())
            .map(String::trim)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    for (String imageName : dedupedNames) {
      ReviewProjectRequestScreenshot screenshot = new ReviewProjectRequestScreenshot();
      screenshot.setReviewProjectRequest(reviewProjectRequest);
      screenshot.setImageName(imageName);
      reviewProjectScreenshotRepository.save(screenshot);
    }
  }

  private List<TextUnitDTO> searchReviewCandidates(List<Long> tmTextUnitIds) {
    if (tmTextUnitIds == null || tmTextUnitIds.isEmpty()) {
      throw new IllegalArgumentException("tmTextUnitIds must be provided");
    }

    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setTmTextUnitIds(tmTextUnitIds);
    params.setPluralFormsFiltered(false);
    params.setOffset(0);
    params.setLimit(tmTextUnitIds.size());

    return textUnitSearcher.search(params);
  }

  private void populateProjectWithTextUnits(
      ReviewProject reviewProject, List<TextUnitDTO> candidates) {

    int selectedCount = 0;
    int totalWordCount = 0;

    for (TextUnitDTO candidate : candidates) {
      Long variantId = candidate.getTmTextUnitVariantId();
      if (variantId == null) {
        continue;
      }

      TMTextUnitVariant variant = entityManager.getReference(TMTextUnitVariant.class, variantId);

      TMTextUnit tmTextUnit =
          entityManager.getReference(TMTextUnit.class, candidate.getTmTextUnitId());

      ReviewProjectTextUnit reviewProjectTextUnit = new ReviewProjectTextUnit();
      reviewProjectTextUnit.setReviewProject(reviewProject);
      reviewProjectTextUnit.setTmTextUnitVariant(variant);
      reviewProjectTextUnit.setTmTextUnit(tmTextUnit);

      reviewProjectTextUnitRepository.save(reviewProjectTextUnit);
      selectedCount++;
      totalWordCount += computeWordCount(candidate.getSource());
    }

    reviewProject.setTextUnitCount(selectedCount);
    reviewProject.setWordCount(totalWordCount);
  }

  private int computeWordCount(String text) {
    if (text == null || text.isBlank()) {
      return 0;
    }
    return (int)
        java.util.Arrays.stream(text.trim().split("\\s+")).filter(token -> !token.isEmpty()).count();
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

  private List<String> resolveScreenshotImageNames(ReviewProject project) {
    if (project.getReviewProjectRequest() == null
        || project.getReviewProjectRequest().getId() == null) {
      return Collections.emptyList();
    }
    return new ArrayList<>(
        new LinkedHashSet<>(
            reviewProjectScreenshotRepository.findImageNamesByReviewProjectRequestId(
                project.getReviewProjectRequest().getId())));
  }

  private SearchReviewProjectsView.ReviewProject toSummaryView(
      ReviewProject project, int totalSelected, int wordCount) {
    Locale locale =
        java.util.Objects.requireNonNull(
            project.getLocale(), "ReviewProject must have a locale (DB invariant)");
    String bcp47Tag =
        java.util.Objects.requireNonNull(
            locale.getBcp47Tag(), "ReviewProject locale must have bcp47Tag");

    Long requestId =
        project.getReviewProjectRequest() != null
            ? project.getReviewProjectRequest().getId()
            : null;
    String requestName = resolveRequestName(project);

    return new SearchReviewProjectsView.ReviewProject(
        project.getId(),
        project.getCreatedDate(),
        project.getLastModifiedDate(),
        project.getDueDate(),
        project.getCloseReason(),
        totalSelected,
        wordCount,
        project.getType(),
        project.getStatus(),
        new SearchReviewProjectsView.Locale(locale.getId(), bcp47Tag),
        new SearchReviewProjectsView.ReviewProjectRequest(requestId, requestName));
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

  private List<ReviewProjectTextUnitView> toTextUnitViews(ReviewProject reviewProject) {
    Map<Long, ReviewProjectTextUnitDecision> decisionsByTextUnitId =
        reviewProjectTextUnitDecisionRepository
            .findByReviewProjectTextUnit_ReviewProject_Id(reviewProject.getId())
            .stream()
            .collect(
                Collectors.toMap(
                    decision -> decision.getReviewProjectTextUnit().getId(), decision -> decision));

    return reviewProjectTextUnitRepository
        .findByReviewProjectIdOrderByIdAsc(reviewProject.getId())
        .stream()
        .map(textUnit -> toTextUnitView(textUnit, decisionsByTextUnitId.get(textUnit.getId())))
        .collect(Collectors.toList());
  }

  private ReviewProjectDetail.ReviewProjectTextUnit toDetailTextUnit(
      ReviewProjectTextUnit textUnit, ReviewProjectTextUnitDecision decision) {
    return toDetailTextUnit(toTextUnitView(textUnit, decision));
  }

  private ReviewProjectDetail.ReviewProjectTextUnit toDetailTextUnit(
      ReviewProjectTextUnitView view) {
    ReviewProjectDetail.TmTextUnit tmTextUnit =
        new ReviewProjectDetail.TmTextUnit(
            view.tmTextUnitId(),
            view.name(),
            view.source(),
            view.notes(),
            new ReviewProjectDetail.Asset(null),
            null);

    ReviewProjectDetail.TmTextUnitVariant tmTextUnitVariant =
        new ReviewProjectDetail.TmTextUnitVariant(
            view.tmTextUnitVariantId(),
            view.currentTarget() != null ? view.currentTarget() : view.target(),
            view.status(),
            view.includedInLocalizedFile(),
            view.notes());

    return new ReviewProjectDetail.ReviewProjectTextUnit(
        view.reviewProjectTextUnitId(), tmTextUnit, tmTextUnitVariant);
  }

  private ReviewProjectTextUnitView toTextUnitView(
      ReviewProjectTextUnit textUnit, ReviewProjectTextUnitDecision decision) {
    TMTextUnit tmTextUnit =
        java.util.Objects.requireNonNull(
            textUnit.getTmTextUnit(), "ReviewProjectTextUnit must have a TMTextUnit");

    TMTextUnitVariant selectedVariantRef = textUnit.getTmTextUnitVariant();
    TMTextUnitVariant resolvedVariant =
        decision != null && decision.getVariant() != null
            ? decision.getVariant()
            : selectedVariantRef;

    Long variantId = resolvedVariant != null ? resolvedVariant.getId() : null;
    Long selectedVariantId = selectedVariantRef != null ? selectedVariantRef.getId() : null;
    TMTextUnitVariant.Status baselineStatus =
        selectedVariantRef != null ? selectedVariantRef.getStatus() : null;

    String reviewStatus =
        decision != null && decision.getVariant() != null
            ? (decision.getVariant().getContent() != null
                    && selectedVariantRef != null
                    && selectedVariantRef.getContent() != null
                    && !NormalizationUtils.normalize(selectedVariantRef.getContent())
                        .equals(NormalizationUtils.normalize(decision.getVariant().getContent())))
                ? "ACCEPTED_WITH_CHANGE"
                : "ACCEPTED_AS_IS"
            : "PENDING";

    String notes = decision != null ? decision.getNotes() : null;
    ZonedDateTime reviewedAt =
        decision != null
            ? (decision.getLastModifiedDate() != null
                ? decision.getLastModifiedDate()
                : decision.getCreatedDate())
            : null;
    User decidedBy =
        decision != null
            ? (decision.getLastModifiedByUser() != null
                ? decision.getLastModifiedByUser()
                : decision.getCreatedByUser())
            : null;
    String reviewedBy = decidedBy != null ? decidedBy.getUsername() : null;

    Repository repository =
        tmTextUnit.getAsset() != null ? tmTextUnit.getAsset().getRepository() : null;

    String source = tmTextUnit.getContent(); // stored source string

    return new ReviewProjectTextUnitView(
        textUnit.getId(),
        tmTextUnit.getId(),
        variantId,
        selectedVariantId,
        variantId,
        tmTextUnit.getName(),
        source,
        selectedVariantRef != null ? selectedVariantRef.getContent() : null,
        resolvedVariant != null ? resolvedVariant.getContent() : null,
        resolvedVariant != null && resolvedVariant.getStatus() != null
            ? resolvedVariant.getStatus().name()
            : null,
        baselineStatus != null ? baselineStatus.name() : null,
        reviewStatus,
        notes,
        reviewedAt,
        reviewedBy,
        repository != null ? repository.getId() : null,
        repository != null ? repository.getName() : null,
        tmTextUnit != null && tmTextUnit.getAsset() != null
            ? tmTextUnit.getAsset().getPath()
            : null,
        resolvedVariant != null && resolvedVariant.isIncludedInLocalizedFile());
  }

}
