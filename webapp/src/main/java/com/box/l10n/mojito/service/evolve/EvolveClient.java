package com.box.l10n.mojito.service.evolve;

import com.box.l10n.mojito.iterators.ListWithLastPage;
import com.box.l10n.mojito.iterators.PageFetcherCurrentAndTotalPagesSplitIterator;
import com.box.l10n.mojito.service.evolve.dto.CourseDTO;
import com.box.l10n.mojito.service.evolve.dto.CoursesDTO;
import com.box.l10n.mojito.service.evolve.dto.TranslationStatusType;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class EvolveClient {
  static Logger logger = LoggerFactory.getLogger(EvolveClient.class);

  private final String apiPath;

  private final RestTemplate restTemplate;

  private final int maxRetries;

  private final Duration retryMinBackoff;

  private final Duration retryMaxBackoff;

  public EvolveClient(
      RestTemplate restTemplate,
      String apiPath,
      int maxRetries,
      long retryMinBackoffSecs,
      long retryMaxBackoffSecs) {
    this.restTemplate = restTemplate;
    this.apiPath = apiPath;
    this.maxRetries = maxRetries;
    this.retryMinBackoff = Duration.ofSeconds(retryMinBackoffSecs);
    this.retryMaxBackoff = Duration.ofSeconds(retryMaxBackoffSecs);
  }

  private String getFullEndpointPath(String endpointPath) {
    return this.apiPath + endpointPath;
  }

  private ListWithLastPage<CourseDTO> getCourses(String url) {
    CoursesDTO coursesDTO = this.restTemplate.getForObject(url, CoursesDTO.class);
    if (coursesDTO == null) {
      logger.error("Get Courses response is empty");
      throw new EvolveSyncException("Empty response");
    }
    ListWithLastPage<CourseDTO> courseListWithLastPage = new ListWithLastPage<>();
    courseListWithLastPage.setList(coursesDTO.getCourses());
    courseListWithLastPage.setLastPage(coursesDTO.getPagination().getTotalPages());
    return courseListWithLastPage;
  }

  public Stream<CourseDTO> getCourses(CoursesGetRequest request) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromPath(this.getFullEndpointPath("courses"))
            .queryParam("locale", request.locale())
            .queryParam("is_active", request.active());
    if (request.updatedOnTo() != null) {
      builder.queryParam("updated_on_to", request.updatedOnTo());
    }
    PageFetcherCurrentAndTotalPagesSplitIterator<CourseDTO> iterator =
        new PageFetcherCurrentAndTotalPagesSplitIterator<>(
            pageToFetch -> {
              UriComponentsBuilder builderWithPage =
                  builder.cloneBuilder().queryParam("page", pageToFetch);
              return Mono.fromCallable(() -> this.getCourses(builderWithPage.toUriString()))
                  .retryWhen(
                      Retry.backoff(this.maxRetries, this.retryMinBackoff)
                          .maxBackoff(this.retryMaxBackoff))
                  .doOnError(e -> logger.info("Unable to fetch courses", e))
                  .block();
            },
            1);

    return StreamSupport.stream(iterator, false);
  }

  /**
   * Update stored translations for Evolve cloud courses so that subsequent translation requests can
   * be made. This is called when starting translation for 'CourseEvolve'-type courses.
   *
   * @param courseId The ID of the course
   * @return The status of the request
   */
  public Map<?, ?> syncEvolve(int courseId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return this.restTemplate.postForObject(
        this.getFullEndpointPath("course_translations/{courseId}/evolve_sync"),
        headers,
        Map.class,
        courseId);
  }

  public String startCourseTranslation(
      int courseId, String targetLocale, Set<String> additionalLocales) {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_XML));
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromPath(this.getFullEndpointPath("course_translations/{courseId}"))
            .queryParam("target_locale", targetLocale);
    if (additionalLocales != null && !additionalLocales.isEmpty()) {
      builder.queryParam("additional_locales[]", additionalLocales);
    }

    HttpEntity<String> httpEntity = new HttpEntity<>(headers);
    return this.restTemplate.postForObject(
        builder.buildAndExpand(courseId).toUriString(), httpEntity, String.class);
  }

  public void updateCourse(
      int courseId, TranslationStatusType translationStatus, ZonedDateTime ifUnmodifiedSince) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setIfModifiedSince(ifUnmodifiedSince);
    Map<String, String> course = ImmutableMap.of("custom_j", translationStatus.getName());
    Map<String, Map<String, String>> courseBody = ImmutableMap.of("course", course);
    this.restTemplate.put(
        this.getFullEndpointPath("courses/{courseId}"),
        new HttpEntity<>(courseBody, headers),
        courseId);
  }

  public void updateCourseTranslation(int courseId, String translatedCourse) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.valueOf("application/xml;charset=UTF-8"));
    headers.setAccept(List.of(MediaType.APPLICATION_XML));
    this.restTemplate.put(
        this.getFullEndpointPath("course_translations/{courseId}"),
        new HttpEntity<>(translatedCourse, headers),
        courseId);
  }
}
