package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.*;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Locale_;
import com.box.l10n.mojito.entity.review.*;
import com.box.l10n.mojito.entity.review.ReviewProject;
import com.box.l10n.mojito.entity.review.ReviewProjectRequest;
import com.box.l10n.mojito.entity.review.ReviewProjectRequest_;
import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProject_;
import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.WordCountService;
import com.box.l10n.mojito.service.locale.LocaleService;
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
      WordCountService wordCountService) {
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

    for (String localeTag : request.localeTags()) {
      Locale locale = localeService.findByBcp47Tag(localeTag);

      if (locale == null) {
        throw new IllegalArgumentException("Unknown locale: " + localeTag);
      }

      ReviewProject reviewProject = new ReviewProject();
      reviewProject.setType(request.type());
      reviewProject.setStatus(ReviewProjectStatus.OPEN);
      reviewProject.setDueDate(request.dueDate());
      reviewProject.setLocale(locale);
      reviewProject.setReviewProjectRequest(reviewProjectRequest);

      ReviewProject saved = reviewProjectRepository.save(reviewProject);

      int wordCount = 0;
      int textUnitCount = 0;

      List<TextUnitDTO> candidates = searchReviewCandidates(request.tmTextUnitIds(), locale);

      for (TextUnitDTO textUnitDTO : candidates) {
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

    if (request == null) {
      throw new IllegalArgumentException("request must not be null");
    }

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<SearchReviewProjectDetail> cq =
        cb.createQuery(SearchReviewProjectDetail.class);
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

    List<SearchReviewProjectsView.ReviewProject> reviewProjects =
        projectDetails.stream()
            .map(
                detail ->
                    new SearchReviewProjectsView.ReviewProject(
                        detail.id(),
                        detail.createdDate(),
                        detail.lastModifiedDate(),
                        detail.dueDate(),
                        detail.closeReason(),
                        detail.textUnitCount(),
                        detail.wordCount(),
                        detail.type(),
                        detail.status(),
                        new SearchReviewProjectsView.Locale(
                            detail.localeId(), detail.localeTag()),
                        new SearchReviewProjectsView.ReviewProjectRequest(
                            detail.requestId(), detail.requestName())))
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
        };

    return buildStringPredicate(cb, expression, request.searchQuery(), matchType);
  }

  private String escapeForLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  @Transactional(readOnly = true)
  public GetProjectDetailView getProjectDetail(Long projectId) throws EntityWithIdNotFoundException {
    ReviewProjectDetail project =
        reviewProjectRepository
            .findDetailById(projectId)
            .orElseThrow(() -> new EntityWithIdNotFoundException("reviewProject", projectId));

    List<ReviewProjectTextUnitDetail> textUnitDetails =
        reviewProjectTextUnitRepository.findDetailByReviewProjectId(projectId);

    List<GetProjectDetailView.ReviewProjectTextUnit> reviewProjectTextUnits =
        textUnitDetails.stream()
            .map(
                detail -> {
                  GetProjectDetailView.Asset.Repository repository =
                      new GetProjectDetailView.Asset.Repository(
                          detail.repositoryId(), detail.repositoryName());
                  GetProjectDetailView.Asset assetView =
                      new GetProjectDetailView.Asset(detail.assetPath(), repository);
                  GetProjectDetailView.TmTextUnit tmTextUnitView =
                      new GetProjectDetailView.TmTextUnit(
                          detail.tmTextUnitId(),
                          detail.tmTextUnitName(),
                          detail.tmTextUnitContent(),
                          detail.tmTextUnitComment(),
                          assetView,
                          detail.tmTextUnitWordCount() != null
                              ? detail.tmTextUnitWordCount().longValue()
                              : null);
                  GetProjectDetailView.TmTextUnitVariant tmTextUnitVariantView =
                      detail.tmTextUnitVariantId() == null
                          ? new GetProjectDetailView.TmTextUnitVariant(
                              null, null, null, false, null)
                          : new GetProjectDetailView.TmTextUnitVariant(
                              detail.tmTextUnitVariantId(),
                              detail.tmTextUnitVariantContent(),
                              detail.tmTextUnitVariantStatus() != null
                                  ? detail.tmTextUnitVariantStatus().name()
                                  : null,
                              detail.tmTextUnitVariantIncludedInLocalizedFile(),
                              detail.tmTextUnitVariantComment());
                  return new GetProjectDetailView.ReviewProjectTextUnit(
                      detail.reviewProjectTextUnitId(), tmTextUnitView, tmTextUnitVariantView);
                })
            .toList();

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
  public GetProjectDetailView.ReviewProjectTextUnit acceptTextUnit(
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
          expectedCurrentTmTextUnitVariantId, currentVariantId, null);
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

    return null;
  }

  @Transactional
  public GetProjectDetailView.ReviewProjectTextUnit updateReviewStatus(
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

    return null;
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
}
