package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectTextUnitDecision.DecisionState;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;
import com.box.l10n.mojito.service.review.CreateReviewProjectRequestCommand;
import com.box.l10n.mojito.service.review.CreateReviewProjectRequestResult;
import com.box.l10n.mojito.service.review.GetProjectDetailView;
import com.box.l10n.mojito.service.review.ReviewProjectCurrentVariantConflictException;
import com.box.l10n.mojito.service.review.ReviewProjectService;
import com.box.l10n.mojito.service.review.ReviewProjectTextUnitDetail;
import com.box.l10n.mojito.service.review.SearchReviewProjectsCriteria;
import com.box.l10n.mojito.service.review.SearchReviewProjectsView;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ReviewProjectWS {

  private final ReviewProjectService reviewProjectService;

  public ReviewProjectWS(ReviewProjectService reviewProjectService) {
    this.reviewProjectService = reviewProjectService;
  }

  @PostMapping("/review-projects/search")
  public SearchReviewProjectsResponse searchReviewProjects(
      @RequestBody SearchReviewProjectsRequest request) {
    SearchReviewProjectsView view = reviewProjectService.searchReviewProjects(toCriteria(request));
    List<SearchReviewProjectsResponse.ReviewProject> projects =
        view.reviewProject().stream().map(this::toSearchReviewProjectsResponse).toList();
    return new SearchReviewProjectsResponse(projects);
  }

  @PostMapping("/review-project-requests")
  @ResponseStatus(HttpStatus.CREATED)
  public CreateReviewProjectRequestResponse createReviewProjectRequest(
      @RequestBody CreateReviewProjectRequestRequest request) {
    CreateReviewProjectRequestResult result =
        reviewProjectService.createReviewProjectRequest(
            new CreateReviewProjectRequestCommand(
                request.localeTags(),
                request.notes(),
                request.tmTextUnitIds(),
                request.type(),
                request.dueDate(),
                request.screenshotImageIds(),
                request.name()));

    return new CreateReviewProjectRequestResponse(
        result.requestId(),
        result.requestName(),
        result.localeTags(),
        result.dueDate(),
        result.projectIds());
  }

  @GetMapping("/review-projects/{projectId}")
  public GetReviewProjectResponse getReviewProject(@PathVariable Long projectId)
      throws EntityWithIdNotFoundException {
    GetProjectDetailView projectDetail = reviewProjectService.getProjectDetail(projectId);
    return toDetailResponse(projectDetail);
  }

  @PostMapping("/review-projects/{projectId}/status")
  public GetReviewProjectResponse updateReviewProjectStatus(
      @PathVariable Long projectId, @RequestBody UpdateReviewProjectStatusRequest request) {
    if (request == null || request.status() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
    }

    GetProjectDetailView projectDetail =
        reviewProjectService.updateProjectStatus(
            projectId, request.status(), request.closeReason());
    return toDetailResponse(projectDetail);
  }

  @PostMapping("/admin/review-projects/status")
  public AdminBatchActionResponse adminBatchUpdateReviewProjectStatus(
      @RequestBody AdminBatchUpdateStatusRequest request) {
    if (request == null || request.status() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
    }
    if (request.projectIds() == null || request.projectIds().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectIds is required");
    }

    int affected =
        reviewProjectService.adminBatchUpdateStatus(
            request.projectIds(), request.status(), request.closeReason());
    return new AdminBatchActionResponse(affected);
  }

  @PostMapping("/admin/review-projects/delete")
  public AdminBatchActionResponse adminBatchDeleteReviewProjects(
      @RequestBody AdminBatchDeleteRequest request) {
    if (request == null || request.projectIds() == null || request.projectIds().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectIds is required");
    }

    int affected = reviewProjectService.adminBatchDeleteProjects(request.projectIds());
    return new AdminBatchActionResponse(affected);
  }

  @PostMapping("/review-project-text-units/{textUnitId}/decision")
  public ResponseEntity<GetReviewProjectResponse.ReviewProjectTextUnit> saveDecision(
      @PathVariable Long textUnitId, @RequestBody ReviewProjectTextUnitDecisionRequest request)
      throws EntityWithIdNotFoundException {
    DecisionState decisionState = request.getDecisionState();
    if (decisionState == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "decisionState is required");
    }

    try {
      ReviewProjectTextUnitDetail detail =
          reviewProjectService.saveDecision(
              textUnitId,
              request.getTarget(),
              request.getComment(),
              request.getStatus(),
              request.getIncludedInLocalizedFile(),
              decisionState,
              request.getExpectedCurrentTmTextUnitVariantId(),
              Boolean.TRUE.equals(request.getOverrideChangedCurrent()),
              request.getDecisionNotes());
      return ResponseEntity.ok(toTextUnitResponse(detail));
    } catch (ReviewProjectCurrentVariantConflictException conflict) {
      ReviewProjectTextUnitDetail currentTextUnit = conflict.getCurrentTextUnit();
      return currentTextUnit == null
          ? ResponseEntity.status(HttpStatus.CONFLICT).build()
          : ResponseEntity.status(HttpStatus.CONFLICT).body(toTextUnitResponse(currentTextUnit));
    }
  }

  /** Response contract for create review project request (minimal payload). */
  public record CreateReviewProjectRequestResponse(
      Long requestId,
      String requestName,
      List<String> localeTags,
      ZonedDateTime dueDate,
      List<Long> projectIds) {}

  public record UpdateReviewProjectStatusRequest(ReviewProjectStatus status, String closeReason) {}

  public record AdminBatchUpdateStatusRequest(
      List<Long> projectIds, ReviewProjectStatus status, String closeReason) {}

  public record AdminBatchDeleteRequest(List<Long> projectIds) {}

  public record AdminBatchActionResponse(int affectedCount) {}

  /** Summary response used by list/search endpoints. */
  public record SearchReviewProjectsResponse(List<ReviewProject> reviewProjects) {

    public record ReviewProject(
        Long id,
        ZonedDateTime createdDate,
        ZonedDateTime lastModifiedDate,
        ZonedDateTime dueDate,
        String closeReason,
        Integer textUnitCount,
        Integer wordCount,
        Long acceptedCount,
        ReviewProjectType type,
        ReviewProjectStatus status,
        Locale locale,
        ReviewProjectRequest reviewProjectRequest) {
      public record Locale(Long id, String bcp47Tag) {}

      public record ReviewProjectRequest(Long id, String name) {}
    }
  }

  public record GetReviewProjectResponse(
      Long id,
      ReviewProjectType type,
      ReviewProjectStatus status,
      ZonedDateTime createdDate,
      ZonedDateTime dueDate,
      String closeReason,
      Integer textUnitCount,
      Integer wordCount,
      ReviewProjectRequest reviewProjectRequest,
      Locale locale,
      List<ReviewProjectTextUnit> reviewProjectTextUnits) {

    public record ReviewProjectRequest(Long id, String name, List<String> screenshotImageIds) {}

    public record Locale(Long id, String bcp47Tag) {}

    public record ReviewProjectTextUnit(
        Long id,
        TmTextUnit tmTextUnit,
        TmTextUnitVariant baselineTmTextUnitVariant,
        TmTextUnitVariant currentTmTextUnitVariant,
        ReviewProjectTextUnitDecision reviewProjectTextUnitDecision) {}

    public record TmTextUnit(
        Long id, String name, String content, String comment, Asset asset, Long wordCount) {}

    public record Asset(String assetPath, Repository repository) {
      public record Repository(Long id, String name) {}
    }

    public record TmTextUnitVariant(
        Long id, String content, String status, Boolean includedInLocalizedFile, String comment) {}

    public record ReviewProjectTextUnitDecision(
        Long reviewedTmTextUnitVariantId,
        String notes,
        String decisionState,
        TmTextUnitVariant decisionTmTextUnitVariant) {}
  }

  // Mapping helpers
  private SearchReviewProjectsResponse.ReviewProject toSearchReviewProjectsResponse(
      SearchReviewProjectsView.ReviewProject view) {
    return new SearchReviewProjectsResponse.ReviewProject(
        view.id(),
        view.createdDate(),
        view.lastModifiedDate(),
        view.dueDate(),
        view.closeReason(),
        view.textUnitCount(),
        view.wordCount(),
        view.acceptedCount(),
        view.type(),
        view.status(),
        view.locale() != null
            ? new SearchReviewProjectsResponse.ReviewProject.Locale(
                view.locale().id(), view.locale().bcp47Tag())
            : null,
        view.reviewProjectRequest() != null
            ? new SearchReviewProjectsResponse.ReviewProject.ReviewProjectRequest(
                view.reviewProjectRequest().id(), view.reviewProjectRequest().name())
            : null);
  }

  private SearchReviewProjectsCriteria toCriteria(SearchReviewProjectsRequest request) {
    if (request == null) {
      return null;
    }
    return new SearchReviewProjectsCriteria(
        request.statuses(),
        request.types(),
        request.localeTags(),
        request.createdAfter(),
        request.createdBefore(),
        request.dueAfter(),
        request.dueBefore(),
        request.limit(),
        request.searchQuery(),
        Optional.ofNullable(request.searchField())
            .map(
                sf ->
                    switch (sf) {
                      case ID -> SearchReviewProjectsCriteria.SearchField.ID;
                      case NAME -> SearchReviewProjectsCriteria.SearchField.NAME;
                      case REQUEST_ID -> SearchReviewProjectsCriteria.SearchField.REQUEST_ID;
                    })
            .orElse(null),
        Optional.ofNullable(request.searchMatchType())
            .map(
                mt ->
                    switch (mt) {
                      case EXACT -> SearchReviewProjectsCriteria.SearchMatchType.EXACT;
                      case ILIKE -> SearchReviewProjectsCriteria.SearchMatchType.ILIKE;
                      case CONTAINS -> SearchReviewProjectsCriteria.SearchMatchType.CONTAINS;
                    })
            .orElse(null));
  }

  private GetReviewProjectResponse toDetailResponse(GetProjectDetailView detail) {
    return new GetReviewProjectResponse(
        detail.id(),
        detail.type(),
        detail.status(),
        detail.createdDate(),
        detail.dueDate(),
        detail.closeReason(),
        detail.textUnitCount(),
        detail.wordCount(),
        detail.reviewProjectRequest() != null
            ? new GetReviewProjectResponse.ReviewProjectRequest(
                detail.reviewProjectRequest().id(),
                detail.reviewProjectRequest().name(),
                detail.reviewProjectRequest().screenshotImageIds())
            : null,
        detail.locale() != null
            ? new GetReviewProjectResponse.Locale(detail.locale().id(), detail.locale().bcp47Tag())
            : null,
        detail.reviewProjectTextUnits().stream().map(this::toTextUnitResponse).toList());
  }

  private GetReviewProjectResponse.ReviewProjectTextUnit toTextUnitResponse(
      GetProjectDetailView.ReviewProjectTextUnit view) {
    GetProjectDetailView.ReviewProjectTextUnitDecision decision =
        view.reviewProjectTextUnitDecision();
    String decisionStateName =
        decision != null && decision.decisionState() != null
            ? decision.decisionState().name()
            : null;
    GetReviewProjectResponse.TmTextUnitVariant decisionVariant =
        decision != null && decision.decisionTmTextUnitVariant() != null
            ? new GetReviewProjectResponse.TmTextUnitVariant(
                decision.decisionTmTextUnitVariant().id(),
                decision.decisionTmTextUnitVariant().content(),
                decision.decisionTmTextUnitVariant().status(),
                decision.decisionTmTextUnitVariant().includedInLocalizedFile(),
                decision.decisionTmTextUnitVariant().comment())
            : null;
    return new GetReviewProjectResponse.ReviewProjectTextUnit(
        view.id(),
        new GetReviewProjectResponse.TmTextUnit(
            view.tmTextUnit().id(),
            view.tmTextUnit().name(),
            view.tmTextUnit().content(),
            view.tmTextUnit().comment(),
            new GetReviewProjectResponse.Asset(
                view.tmTextUnit().asset().assetPath(),
                new GetReviewProjectResponse.Asset.Repository(
                    view.tmTextUnit().asset().repository().id(),
                    view.tmTextUnit().asset().repository().name())),
            view.tmTextUnit().wordCount()),
        new GetReviewProjectResponse.TmTextUnitVariant(
            view.baselineTmTextUnitVariant().id(),
            view.baselineTmTextUnitVariant().content(),
            view.baselineTmTextUnitVariant().status(),
            view.baselineTmTextUnitVariant().includedInLocalizedFile(),
            view.baselineTmTextUnitVariant().comment()),
        new GetReviewProjectResponse.TmTextUnitVariant(
            view.currentTmTextUnitVariant().id(),
            view.currentTmTextUnitVariant().content(),
            view.currentTmTextUnitVariant().status(),
            view.currentTmTextUnitVariant().includedInLocalizedFile(),
            view.currentTmTextUnitVariant().comment()),
        decision == null
            ? null
            : new GetReviewProjectResponse.ReviewProjectTextUnitDecision(
                decision.reviewedTmTextUnitVariantId(),
                decision.notes(),
                decisionStateName,
                decisionVariant));
  }

  private GetReviewProjectResponse.ReviewProjectTextUnit toTextUnitResponse(
      ReviewProjectTextUnitDetail detail) {
    GetReviewProjectResponse.Asset.Repository repository =
        new GetReviewProjectResponse.Asset.Repository(
            detail.repositoryId(), detail.repositoryName());
    GetReviewProjectResponse.Asset asset =
        new GetReviewProjectResponse.Asset(detail.assetPath(), repository);
    GetReviewProjectResponse.TmTextUnit tmTextUnit =
        new GetReviewProjectResponse.TmTextUnit(
            detail.tmTextUnitId(),
            detail.tmTextUnitName(),
            detail.tmTextUnitContent(),
            detail.tmTextUnitComment(),
            asset,
            detail.tmTextUnitWordCount() != null ? detail.tmTextUnitWordCount().longValue() : null);

    GetReviewProjectResponse.TmTextUnitVariant baselineVariant =
        new GetReviewProjectResponse.TmTextUnitVariant(
            detail.baselineTmTextUnitVariantId(),
            detail.baselineTmTextUnitVariantContent(),
            detail.baselineTmTextUnitVariantStatus() != null
                ? detail.baselineTmTextUnitVariantStatus().name()
                : null,
            detail.baselineTmTextUnitVariantIncludedInLocalizedFile(),
            detail.baselineTmTextUnitVariantComment());
    GetReviewProjectResponse.TmTextUnitVariant currentVariant =
        new GetReviewProjectResponse.TmTextUnitVariant(
            detail.currentTmTextUnitVariantId(),
            detail.currentTmTextUnitVariantContent(),
            detail.currentTmTextUnitVariantStatus() != null
                ? detail.currentTmTextUnitVariantStatus().name()
                : null,
            detail.currentTmTextUnitVariantIncludedInLocalizedFile(),
            detail.currentTmTextUnitVariantComment());
    GetReviewProjectResponse.TmTextUnitVariant decisionVariant =
        detail.decisionVariantId() == null
            ? null
            : new GetReviewProjectResponse.TmTextUnitVariant(
                detail.decisionVariantId(),
                detail.decisionVariantContent(),
                detail.decisionVariantStatus() != null
                    ? detail.decisionVariantStatus().name()
                    : null,
                detail.decisionVariantIncludedInLocalizedFile(),
                detail.decisionVariantComment());

    String decisionStateName =
        detail.decisionState() != null ? detail.decisionState().name() : null;
    boolean hasDecision =
        decisionStateName != null
            || detail.decisionVariantId() != null
            || detail.reviewedTmTextUnitVariantId() != null
            || detail.decisionNotes() != null;
    GetReviewProjectResponse.ReviewProjectTextUnitDecision decision =
        hasDecision
            ? new GetReviewProjectResponse.ReviewProjectTextUnitDecision(
                detail.reviewedTmTextUnitVariantId(),
                detail.decisionNotes(),
                decisionStateName,
                decisionVariant)
            : null;

    return new GetReviewProjectResponse.ReviewProjectTextUnit(
        detail.reviewProjectTextUnitId(), tmTextUnit, baselineVariant, currentVariant, decision);
  }
}
