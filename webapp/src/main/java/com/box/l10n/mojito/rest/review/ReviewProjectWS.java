package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;
import com.box.l10n.mojito.service.review.CreateReviewProjectCommand;
import com.box.l10n.mojito.service.review.ReviewProjectCurrentVariantConflictException;
import com.box.l10n.mojito.service.review.ReviewProjectService;
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
@RequestMapping("/api/review-projects")
public class ReviewProjectWS {

  private final ReviewProjectService reviewProjectService;

  public ReviewProjectWS(ReviewProjectService reviewProjectService) {
    this.reviewProjectService = reviewProjectService;
  }

  @GetMapping
  public List<ReviewProjectSummaryResponse> getOpenProjects() {
    return reviewProjectService.getOpenProjects();
  }

  @PostMapping("/search")
  public List<ReviewProjectSummaryResponse> search(@RequestBody ReviewProjectSearchRequest request) {
    return reviewProjectService.searchProjects(request);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReviewProjectCreateResponse createReviewProject(
      @RequestBody ReviewProjectCreateRequest request) {
    return new ReviewProjectCreateResponse(
        reviewProjectService.createReviewProject(
            new CreateReviewProjectCommand(
                request.localeTags(),
                request.notes(),
                request.tmTextUnitIds(),
                request.type(),
                request.dueDate(),
                request.screenshotImageIds(),
                request.name())));
  }

  @GetMapping("/{projectId}")
  public ReviewProjectDetailResponse getProject(@PathVariable Long projectId)
      throws EntityWithIdNotFoundException {
    return reviewProjectService.getProjectDetail(projectId);
  }

  @PostMapping("/{projectId}/text-units/{textUnitId}/accept")
  public ResponseEntity<ReviewProjectTextUnitResponse> acceptTextUnit(
      @PathVariable Long projectId,
      @PathVariable Long textUnitId,
      @RequestBody ReviewProjectTextUnitAcceptRequest request)
      throws EntityWithIdNotFoundException {
    try {
      ReviewProjectTextUnitResponse dto =
          reviewProjectService.acceptTextUnit(
              projectId,
              textUnitId,
              request.getTarget(),
              request.getIncludedInLocalizedFile(),
              request.getExpectedCurrentTmTextUnitVariantId(),
              Boolean.TRUE.equals(request.getOverrideChangedCurrent()),
              request.getNotes());
      return ResponseEntity.ok(dto);
    } catch (ReviewProjectCurrentVariantConflictException conflict) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(conflict.getCurrentTextUnit());
    }
  }

  @PostMapping("/{projectId}/text-units/{textUnitId}/review")
  public ReviewProjectTextUnitResponse updateReviewStatus(
      @PathVariable Long projectId,
      @PathVariable Long textUnitId,
      @RequestBody ReviewProjectTextUnitReviewRequest request)
      throws EntityWithIdNotFoundException {
    return reviewProjectService.updateReviewStatus(projectId, textUnitId, request.getNotes());
  }

  /** Response contract for create review project. */
  public record ReviewProjectCreateResponse(List<ReviewProjectSummaryResponse> projects) {}

  /** Response contract for project summary. */
  public record ReviewProjectSummaryResponse(
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
      List<ReviewProjectRepositorySummaryResponse> repositories,
      List<ReviewProjectLocaleSummaryResponse> locales,
      List<String> screenshotImageIds) {}

  /** Response contract for project detail. */
  public static class ReviewProjectDetailResponse {
    private Long id;
    private ReviewProjectType type;
    private ReviewProjectStatus status;
    private ZonedDateTime createdDate;
    private ZonedDateTime dueDate;
    private String closeReason;
    private Integer textUnitCount;
    private Integer wordCount;
    private String name;
    private String notes;
    private Long requestId;
    private String requestUuid;
    private String requestName;
    private ReviewProjectLocaleDetailResponse locale;
    private List<ReviewProjectRepositorySummaryResponse> repositories;
    private List<ReviewProjectLocaleDetailResponse> locales;
    private List<String> screenshotImageIds;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public ReviewProjectType getType() {
      return type;
    }

    public void setType(ReviewProjectType type) {
      this.type = type;
    }

    public ReviewProjectStatus getStatus() {
      return status;
    }

    public void setStatus(ReviewProjectStatus status) {
      this.status = status;
    }

    public ZonedDateTime getCreatedDate() {
      return createdDate;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
      this.createdDate = createdDate;
    }

    public ZonedDateTime getDueDate() {
      return dueDate;
    }

    public void setDueDate(ZonedDateTime dueDate) {
      this.dueDate = dueDate;
    }

    public String getCloseReason() {
      return closeReason;
    }

    public void setCloseReason(String closeReason) {
      this.closeReason = closeReason;
    }

    public Integer getTextUnitCount() {
      return textUnitCount;
    }

    public void setTextUnitCount(Integer textUnitCount) {
      this.textUnitCount = textUnitCount;
    }

    public Integer getWordCount() {
      return wordCount;
    }

    public void setWordCount(Integer wordCount) {
      this.wordCount = wordCount;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getNotes() {
      return notes;
    }

    public void setNotes(String notes) {
      this.notes = notes;
    }

    public Long getRequestId() {
      return requestId;
    }

    public void setRequestId(Long requestId) {
      this.requestId = requestId;
    }

    public String getRequestUuid() {
      return requestUuid;
    }

    public void setRequestUuid(String requestUuid) {
      this.requestUuid = requestUuid;
    }

    public String getRequestName() {
      return requestName;
    }

    public void setRequestName(String requestName) {
      this.requestName = requestName;
    }

    public ReviewProjectLocaleDetailResponse getLocale() {
      return locale;
    }

    public void setLocale(ReviewProjectLocaleDetailResponse locale) {
      this.locale = locale;
    }

    public List<ReviewProjectRepositorySummaryResponse> getRepositories() {
      return repositories;
    }

    public void setRepositories(List<ReviewProjectRepositorySummaryResponse> repositories) {
      this.repositories = repositories;
    }

    public List<ReviewProjectLocaleDetailResponse> getLocales() {
      return locales;
    }

    public void setLocales(List<ReviewProjectLocaleDetailResponse> locales) {
      this.locales = locales;
    }

    public List<String> getScreenshotImageIds() {
      return screenshotImageIds;
    }

    public void setScreenshotImageIds(List<String> screenshotImageIds) {
      this.screenshotImageIds = screenshotImageIds;
    }
  }

  /** Locale summary response. */
  public static class ReviewProjectLocaleSummaryResponse {
    private Long id;
    private String bcp47Tag;
    private String displayName;
    private int selectedCount;
    private long acceptedCount;

    public ReviewProjectLocaleSummaryResponse() {}

    public ReviewProjectLocaleSummaryResponse(
        Long id, String bcp47Tag, String displayName, int selectedCount, long acceptedCount) {
      this.id = id;
      this.bcp47Tag = bcp47Tag;
      this.displayName = displayName;
      this.selectedCount = selectedCount;
      this.acceptedCount = acceptedCount;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getBcp47Tag() {
      return bcp47Tag;
    }

    public void setBcp47Tag(String bcp47Tag) {
      this.bcp47Tag = bcp47Tag;
    }

    public String getDisplayName() {
      return displayName;
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }

    public int getSelectedCount() {
      return selectedCount;
    }

    public void setSelectedCount(int selectedCount) {
      this.selectedCount = selectedCount;
    }

    public long getAcceptedCount() {
      return acceptedCount;
    }

    public void setAcceptedCount(long acceptedCount) {
      this.acceptedCount = acceptedCount;
    }
  }

  /** Locale detail response. */
  public static class ReviewProjectLocaleDetailResponse extends ReviewProjectLocaleSummaryResponse {

    private List<ReviewProjectTextUnitResponse> textUnits;

    public ReviewProjectLocaleDetailResponse() {}

    public ReviewProjectLocaleDetailResponse(
        Long id,
        String bcp47Tag,
        String displayName,
        int selectedCount,
        long acceptedCount,
        List<ReviewProjectTextUnitResponse> textUnits) {
      super(id, bcp47Tag, displayName, selectedCount, acceptedCount);
      this.textUnits = textUnits;
    }

    public List<ReviewProjectTextUnitResponse> getTextUnits() {
      return textUnits;
    }

    public void setTextUnits(List<ReviewProjectTextUnitResponse> textUnits) {
      this.textUnits = textUnits;
    }
  }

  /** Repository summary response. */
  public static class ReviewProjectRepositorySummaryResponse {
    private Long id;
    private String name;

    public ReviewProjectRepositorySummaryResponse() {}

    public ReviewProjectRepositorySummaryResponse(Long id, String name) {
      this.id = id;
      this.name = name;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  /** Text unit response. */
  public static class ReviewProjectTextUnitResponse {
    private Long reviewProjectTextUnitId;
    private Long tmTextUnitId;
    private Long tmTextUnitVariantId;
    private Long selectedTmTextUnitVariantId;
    private Long currentTmTextUnitVariantId;
    private String name;
    private String source;
    private String target;
    private String currentTarget;
    private String status;
    private String baselineStatus;
    private String reviewStatus;
    private String notes;
    private ZonedDateTime reviewedAt;
    private String reviewedBy;
    private Long repositoryId;
    private String repositoryName;
    private String assetPath;
    private boolean includedInLocalizedFile;

    public Long getReviewProjectTextUnitId() {
      return reviewProjectTextUnitId;
    }

    public void setReviewProjectTextUnitId(Long reviewProjectTextUnitId) {
      this.reviewProjectTextUnitId = reviewProjectTextUnitId;
    }

    public Long getTmTextUnitId() {
      return tmTextUnitId;
    }

    public void setTmTextUnitId(Long tmTextUnitId) {
      this.tmTextUnitId = tmTextUnitId;
    }

    public Long getTmTextUnitVariantId() {
      return tmTextUnitVariantId;
    }

    public void setTmTextUnitVariantId(Long tmTextUnitVariantId) {
      this.tmTextUnitVariantId = tmTextUnitVariantId;
    }

    public Long getSelectedTmTextUnitVariantId() {
      return selectedTmTextUnitVariantId;
    }

    public void setSelectedTmTextUnitVariantId(Long selectedTmTextUnitVariantId) {
      this.selectedTmTextUnitVariantId = selectedTmTextUnitVariantId;
    }

    public Long getCurrentTmTextUnitVariantId() {
      return currentTmTextUnitVariantId;
    }

    public void setCurrentTmTextUnitVariantId(Long currentTmTextUnitVariantId) {
      this.currentTmTextUnitVariantId = currentTmTextUnitVariantId;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getSource() {
      return source;
    }

    public void setSource(String source) {
      this.source = source;
    }

    public String getTarget() {
      return target;
    }

    public void setTarget(String target) {
      this.target = target;
    }

    public String getCurrentTarget() {
      return currentTarget;
    }

    public void setCurrentTarget(String currentTarget) {
      this.currentTarget = currentTarget;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public String getBaselineStatus() {
      return baselineStatus;
    }

    public void setBaselineStatus(String baselineStatus) {
      this.baselineStatus = baselineStatus;
    }

    public String getReviewStatus() {
      return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
      this.reviewStatus = reviewStatus;
    }

    public String getNotes() {
      return notes;
    }

    public void setNotes(String notes) {
      this.notes = notes;
    }

    public ZonedDateTime getReviewedAt() {
      return reviewedAt;
    }

    public void setReviewedAt(ZonedDateTime reviewedAt) {
      this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
      return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
      this.reviewedBy = reviewedBy;
    }

    public Long getRepositoryId() {
      return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
      this.repositoryId = repositoryId;
    }

    public String getRepositoryName() {
      return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
      this.repositoryName = repositoryName;
    }

    public String getAssetPath() {
      return assetPath;
    }

    public void setAssetPath(String assetPath) {
      this.assetPath = assetPath;
    }

    public boolean isIncludedInLocalizedFile() {
      return includedInLocalizedFile;
    }

    public void setIncludedInLocalizedFile(boolean includedInLocalizedFile) {
      this.includedInLocalizedFile = includedInLocalizedFile;
    }
  }
}
