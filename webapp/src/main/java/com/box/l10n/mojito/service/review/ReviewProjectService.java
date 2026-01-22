package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.*;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Locale_;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.review.*;
import com.box.l10n.mojito.entity.review.ReviewProject;
import com.box.l10n.mojito.entity.review.ReviewProjectRequest;
import com.box.l10n.mojito.entity.review.ReviewProjectRequest_;
import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProject_;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.WordCountService;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
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
  private final TMTextUnitVariantRepository tmTextUnitVariantRepository;
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
      TMTextUnitVariantRepository tmTextUnitVariantRepository,
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
    this.tmTextUnitVariantRepository = tmTextUnitVariantRepository;
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
    List<TextUnitDTO> candidates = searchReviewCandidates(request.tmTextUnitIds());

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
    CriteriaQuery<ReviewProject> cq = cb.createQuery(ReviewProject.class);
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
      String pattern = "%" + request.searchQuery().replace("%", "\\%").replace("_", "\\_") + "%";
      Predicate searchPredicate =
          cb.like(requestJoin.get(ReviewProjectRequest_.name), pattern, '\\');
      predicates.add(searchPredicate);
    }

    Predicate[] predicateArray = predicates.toArray(Predicate[]::new);

    cq.where(predicateArray)
        .select(root)
        .distinct(true)
        .orderBy(cb.desc(root.get(ReviewProject_.id)));

    TypedQuery<ReviewProject> query = entityManager.createQuery(cq);
    query.setMaxResults(request.limit());

    List<ReviewProject> projects = query.getResultList();

    List<SearchReviewProjectsView.ReviewProject> reviewProjects = new ArrayList<>();

    for (ReviewProject project : projects) {
      Locale locale = localeService.findById(project.getLocale().getId());
      reviewProjects.add(
          new SearchReviewProjectsView.ReviewProject(
              project.getId(),
              project.getCreatedDate(),
              project.getLastModifiedDate(),
              project.getDueDate(),
              project.getCloseReason(),
              project.getTextUnitCount(),
              project.getWordCount(),
              project.getType(),
              project.getStatus(),
              new SearchReviewProjectsView.Locale(locale.getId(), locale.getBcp47Tag()),
              new SearchReviewProjectsView.ReviewProjectRequest(
                  project.getReviewProjectRequest().getId(),
                  project
                      .getReviewProjectRequest()
                      .getName())));
    }

    return new SearchReviewProjectsView(reviewProjects);
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
