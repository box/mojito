package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;
import com.box.l10n.mojito.service.review.ReviewProjectCurrentVariantConflictException;
import com.box.l10n.mojito.service.review.ReviewProjectService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
  public List<ReviewProjectSummaryDTO> getOpenProjects() {
    return reviewProjectService.getOpenProjects();
  }

  @PostMapping("/search")
  public List<ReviewProjectSummaryDTO> search(@RequestBody ReviewProjectSearchRequest request) {
    return reviewProjectService.searchProjects(request);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public List<ReviewProjectSummaryDTO> createReviewProject(
      @RequestBody ReviewProjectCreateRequest request) {
    return reviewProjectService.createReviewProject(request);
  }

  @PostMapping("/generate-sample")
  public List<ReviewProjectSummaryDTO> generateSampleProjects(
      @RequestParam(value = "count", required = false) Integer count) {
    return reviewProjectService.generateSampleProjects(count == null ? 50 : count);
  }

  @GetMapping("/generate-sample")
  public List<ReviewProjectSummaryDTO> generateSampleProjectsGet(
      @RequestParam(value = "count", required = false) Integer count) {
    return reviewProjectService.generateSampleProjects(count == null ? 50 : count);
  }

  @GetMapping("/{projectId}")
  public ReviewProjectDetailDTO getProject(@PathVariable Long projectId)
      throws EntityWithIdNotFoundException {
    return reviewProjectService.getProjectDetail(projectId);
  }

  @PostMapping("/{projectId}/text-units/{textUnitId}/accept")
  public ResponseEntity<ReviewProjectTextUnitDTO> acceptTextUnit(
      @PathVariable Long projectId,
      @PathVariable Long textUnitId,
      @RequestBody ReviewProjectTextUnitAcceptRequest request)
      throws EntityWithIdNotFoundException {
    try {
      ReviewProjectTextUnitDTO dto =
          reviewProjectService.acceptTextUnit(
              projectId,
              textUnitId,
              request.getTarget(),
              request.getIncludedInLocalizedFile(),
              request.getExpectedCurrentTmTextUnitVariantId(),
              Boolean.TRUE.equals(request.getOverrideChangedCurrent()));
      return ResponseEntity.ok(dto);
    } catch (ReviewProjectCurrentVariantConflictException conflict) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(conflict.getCurrentTextUnit());
    }
  }
}
