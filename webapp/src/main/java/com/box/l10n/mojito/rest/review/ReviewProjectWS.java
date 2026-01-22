package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;
import com.box.l10n.mojito.service.review.CreateReviewProjectRequestCommand;
import com.box.l10n.mojito.service.review.CreateReviewProjectRequestResult;
import com.box.l10n.mojito.service.review.ReviewProjectCurrentVariantConflictException;
import com.box.l10n.mojito.service.review.ReviewProjectDetail;
import com.box.l10n.mojito.service.review.ReviewProjectService;
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
    return toDetailResponse(reviewProjectService.getProjectDetail(projectId));
  }

  @PostMapping("/review-projects/{projectId}/text-units/{textUnitId}/accept")
  public ResponseEntity<GetReviewProjectResponse.ReviewProjectTextUnit> acceptTextUnit(
      @PathVariable Long projectId,
      @PathVariable Long textUnitId,
      @RequestBody ReviewProjectTextUnitAcceptRequest request)
      throws EntityWithIdNotFoundException {
    try {
      ReviewProjectDetail.ReviewProjectTextUnit view =
          reviewProjectService.acceptTextUnit(
              projectId,
              textUnitId,
              request.getTarget(),
              request.getIncludedInLocalizedFile(),
              request.getExpectedCurrentTmTextUnitVariantId(),
              Boolean.TRUE.equals(request.getOverrideChangedCurrent()),
              request.getNotes());
      return ResponseEntity.ok(toTextUnitResponse(view));
    } catch (ReviewProjectCurrentVariantConflictException conflict) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(toTextUnitResponse(conflict.getCurrentTextUnit()));
    }
  }

  @PostMapping("/review-projects/{projectId}/text-units/{textUnitId}/review")
  public GetReviewProjectResponse.ReviewProjectTextUnit updateReviewStatus(
      @PathVariable Long projectId,
      @PathVariable Long textUnitId,
      @RequestBody ReviewProjectTextUnitReviewRequest request)
      throws EntityWithIdNotFoundException {
    return toTextUnitResponse(
        reviewProjectService.updateReviewStatus(projectId, textUnitId, request.getNotes()));
  }

  /** Response contract for create review project request (minimal payload). */
  public record CreateReviewProjectRequestResponse(
      Long requestId,
      String requestName,
      List<String> localeTags,
      ZonedDateTime dueDate,
      List<Long> projectIds) {}

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
        Long id, TmTextUnit tmTextUnit, TmTextUnitVariant tmTextUnitVariant) {}

    public record TmTextUnit(
        Long id, String name, String content, String comment, Asset asset, Long wordCount) {}

    public record Asset(String assetPath, Repository repository) {
      public record Repository(Long id, String name) {}
    }

    public record TmTextUnitVariant(
        Long id, String content, String status, boolean includedInLocalizedFile, String comment) {}
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

  private GetReviewProjectResponse toDetailResponse(ReviewProjectDetail detail) {
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
      ReviewProjectDetail.ReviewProjectTextUnit view) {
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
            view.tmTextUnitVariant().id(),
            view.tmTextUnitVariant().content(),
            view.tmTextUnitVariant().status(),
            view.tmTextUnitVariant().includedInLocalizedFile(),
            view.tmTextUnitVariant().comment()));
  }
}
