package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;
import com.box.l10n.mojito.service.review.CreateReviewProjectRequestCommand;
import com.box.l10n.mojito.service.review.CreateReviewProjectRequestResult;
import com.box.l10n.mojito.service.review.ReviewProjectCurrentVariantConflictException;
import com.box.l10n.mojito.service.review.ReviewProjectDetailView;
import com.box.l10n.mojito.service.review.ReviewProjectLocaleDetailView;
import com.box.l10n.mojito.service.review.ReviewProjectService;
import com.box.l10n.mojito.service.review.ReviewProjectSummaryView;
import com.box.l10n.mojito.service.review.ReviewProjectTextUnitView;
import java.time.ZonedDateTime;
import java.util.List;
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
  public List<SearchReviewProjectsResponse> searchReviewProjects(@RequestBody SearchReviewProjectsRequest request) {
    return reviewProjectService.searchProjects(request).stream().map(this::toSearchReviewProjectsResponse).toList();
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
        result.requestUuid(),
        result.requestName(),
        result.localeTags(),
        result.dueDate(),
        result.projectIds());
  }

  @GetMapping("/review-projects/{projectId}")
  public ReviewProjectDetailResponse getProject(@PathVariable Long projectId)
      throws EntityWithIdNotFoundException {
    return toDetailResponse(reviewProjectService.getProjectDetail(projectId));
  }

  @PostMapping("/review-projects/{projectId}/text-units/{textUnitId}/accept")
  public ResponseEntity<ReviewProjectDetailResponse.TextUnit> acceptTextUnit(
      @PathVariable Long projectId,
      @PathVariable Long textUnitId,
      @RequestBody ReviewProjectTextUnitAcceptRequest request)
      throws EntityWithIdNotFoundException {
    try {
      ReviewProjectTextUnitView view =
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
  public ReviewProjectDetailResponse.TextUnit updateReviewStatus(
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
      String requestUuid,
      String requestName,
      List<String> localeTags,
      ZonedDateTime dueDate,
      List<Long> projectIds) {}

  /** Summary response used by list/search endpoints. */
  public record SearchReviewProjectsResponse(
      Long id,
      ZonedDateTime createdDate,
      ZonedDateTime dueDate,
      String closeReason,
      Integer textUnitCount,
      Integer wordCount,
      ReviewProjectType type,
      ReviewProjectStatus status,
      Long requestId,
      String requestUuid,
      String requestName,
      int totalSelected,
      long acceptedCount,
      String name,
      List<Repository> repositories,
      List<LocaleSummary> locales,
      List<String> screenshotImageIds) {

    public record Repository(Long id, String name) {}

    public record LocaleSummary(
        Long id, String bcp47Tag, String displayName, int selectedCount, long acceptedCount) {}
  }

  /** Response contract for project detail. */
  /** Response contract for project detail. */
  public record ReviewProjectDetailResponse(
      Long id,
      ReviewProjectType type,
      ReviewProjectStatus status,
      ZonedDateTime createdDate,
      ZonedDateTime dueDate,
      String closeReason,
      Integer textUnitCount,
      Integer wordCount,
      String name,
      String notes,
      Long requestId,
      String requestUuid,
      String requestName,
      LocaleDetail locale,
      List<SearchReviewProjectsResponse.Repository> repositories,
      List<LocaleDetail> locales,
      List<String> screenshotImageIds) {

    public record LocaleDetail(
        Long id,
        String bcp47Tag,
        String displayName,
        int selectedCount,
        long acceptedCount,
        List<TextUnit> textUnits) {}

    public record TextUnit(
        Long reviewProjectTextUnitId,
        Long tmTextUnitId,
        Long tmTextUnitVariantId,
        Long selectedTmTextUnitVariantId,
        Long currentTmTextUnitVariantId,
        String name,
        String source,
        String target,
        String currentTarget,
        String status,
        String baselineStatus,
        String reviewStatus,
        String notes,
        ZonedDateTime reviewedAt,
        String reviewedBy,
        Long repositoryId,
        String repositoryName,
        String assetPath,
        boolean includedInLocalizedFile) {}
  }

  // Mapping helpers
  private SearchReviewProjectsResponse toSearchReviewProjectsResponse(ReviewProjectSummaryView view) {
    return new SearchReviewProjectsResponse(
        view.id(),
        view.createdDate(),
        view.dueDate(),
        view.closeReason(),
        view.textUnitCount(),
        view.wordCount(),
        view.type(),
        view.status(),
        view.requestId(),
        view.requestUuid(),
        view.requestName(),
        view.totalSelected(),
        view.acceptedCount(),
        view.name(),
        view.repositories().stream()
            .map(r -> new SearchReviewProjectsResponse.Repository(r.id(), r.name()))
            .toList(),
        view.locales().stream()
            .map(
                l ->
                    new SearchReviewProjectsResponse.LocaleSummary(
                        l.id(), l.bcp47Tag(), l.displayName(), l.selectedCount(), l.acceptedCount()))
            .toList(),
        view.screenshotImageIds());
  }

  private ReviewProjectDetailResponse toDetailResponse(ReviewProjectDetailView view) {
    ReviewProjectDetailResponse.LocaleDetail localeDetail =
        toLocaleDetail(view.locale());

    return new ReviewProjectDetailResponse(
        view.id(),
        view.type(),
        view.status(),
        view.createdDate(),
        view.dueDate(),
        view.closeReason(),
        view.textUnitCount(),
        view.wordCount(),
        view.name(),
        view.notes(),
        view.requestId(),
        view.requestUuid(),
        view.requestName(),
        localeDetail,
        view.repositories().stream()
            .map(r -> new SearchReviewProjectsResponse.Repository(r.id(), r.name()))
            .toList(),
        view.locales().stream().map(this::toLocaleDetail).toList(),
        view.screenshotImageIds());
  }

  private ReviewProjectDetailResponse.LocaleDetail toLocaleDetail(
      ReviewProjectLocaleDetailView localeView) {
    return new ReviewProjectDetailResponse.LocaleDetail(
        localeView.id(),
        localeView.bcp47Tag(),
        localeView.displayName(),
        localeView.selectedCount(),
        localeView.acceptedCount(),
        localeView.textUnits().stream().map(this::toTextUnitResponse).toList());
  }

  private ReviewProjectDetailResponse.TextUnit toTextUnitResponse(ReviewProjectTextUnitView view) {
    return new ReviewProjectDetailResponse.TextUnit(
        view.reviewProjectTextUnitId(),
        view.tmTextUnitId(),
        view.tmTextUnitVariantId(),
        view.selectedTmTextUnitVariantId(),
        view.currentTmTextUnitVariantId(),
        view.name(),
        view.source(),
        view.target(),
        view.currentTarget(),
        view.status(),
        view.baselineStatus(),
        view.reviewStatus(),
        view.notes(),
        view.reviewedAt(),
        view.reviewedBy(),
        view.repositoryId(),
        view.repositoryName(),
        view.assetPath(),
        view.includedInLocalizedFile());
  }
}
