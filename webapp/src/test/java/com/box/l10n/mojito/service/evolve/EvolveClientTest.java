package com.box.l10n.mojito.service.evolve;

import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.IN_TRANSLATION;
import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.READY_FOR_TRANSLATION;
import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.TRANSLATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.service.evolve.dto.CourseDTO;
import com.box.l10n.mojito.service.evolve.dto.CoursesDTO;
import com.box.l10n.mojito.service.evolve.dto.PaginationDTO;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {EvolveClientTest.class})
public class EvolveClientTest {
  final String apiPath = "api/v3/";

  final int maxRetries = 2;

  final long retryMinBackoffSecs = 1;

  final long retryMaxBackoffSecs = 1;

  @Mock RestTemplate mockRestTemplate;

  EvolveClient evolveClient;

  @Captor ArgumentCaptor<String> urlCaptor;

  @Captor ArgumentCaptor<HttpEntity<Object>> httpEntityCaptor;

  @Captor ArgumentCaptor<Integer> courseIdCaptor;

  private void initDataWithAllCourseTypes() {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setType("CourseEvolve");

    CourseDTO courseDTO2 = new CourseDTO();
    courseDTO2.setId(2);
    courseDTO2.setTranslationStatus(IN_TRANSLATION);
    courseDTO2.setType("CourseCurriculum");

    CourseDTO courseDTO3 = new CourseDTO();
    courseDTO3.setId(3);
    courseDTO3.setTranslationStatus(TRANSLATED);
    courseDTO1.setType("CourseCurriculum");

    CourseDTO courseDTO4 = new CourseDTO();
    courseDTO4.setId(4);
    courseDTO4.setTranslationStatus(null);
    courseDTO1.setType("CourseCurriculum");

    CoursesDTO coursesDTO1 = new CoursesDTO();
    coursesDTO1.setCourses(ImmutableList.of(courseDTO1, courseDTO2, courseDTO3));
    PaginationDTO pagination1 = new PaginationDTO();
    pagination1.setCurrentPage(1);
    pagination1.setTotalPages(2);
    coursesDTO1.setPagination(pagination1);

    CoursesDTO coursesDTO2 = new CoursesDTO();
    coursesDTO2.setCourses(ImmutableList.of(courseDTO4));
    PaginationDTO pagination2 = new PaginationDTO();
    pagination2.setCurrentPage(2);
    pagination2.setTotalPages(2);
    coursesDTO2.setPagination(pagination2);
    when(this.mockRestTemplate.getForObject(anyString(), any()))
        .thenReturn(coursesDTO1)
        .thenReturn(coursesDTO2);
    this.evolveClient =
        new EvolveClient(
            this.mockRestTemplate,
            this.apiPath,
            this.maxRetries,
            this.retryMinBackoffSecs,
            this.retryMaxBackoffSecs);
  }

  @Test
  public void testGetCoursesWithAllCourseTypes() {
    this.initDataWithAllCourseTypes();
    CoursesGetRequest coursesGetRequest = new CoursesGetRequest("en", null);

    List<CourseDTO> courses = this.evolveClient.getCourses(coursesGetRequest).toList();

    verify(this.mockRestTemplate, times(2)).getForObject(urlCaptor.capture(), any());

    assertEquals(urlCaptor.getAllValues().size(), 2);
    assertEquals(
        this.apiPath + "courses?locale=en&is_active=true&page=1",
        urlCaptor.getAllValues().getFirst());
    assertEquals(
        this.apiPath + "courses?locale=en&is_active=true&page=2",
        urlCaptor.getAllValues().getLast());

    for (CourseDTO courseDTO : courses) {
      if (courseDTO.getTranslationStatus() == null) {
        assertEquals(4, courseDTO.getId());
      } else if (courseDTO.getTranslationStatus() == READY_FOR_TRANSLATION) {
        assertEquals(1, courseDTO.getId());
      } else if (courseDTO.getTranslationStatus() == IN_TRANSLATION) {
        assertEquals(2, courseDTO.getId());
      } else if (courseDTO.getTranslationStatus() == TRANSLATED) {
        assertEquals(3, courseDTO.getId());
      }
    }
  }

  private void initEmptyCoursesData() {
    CoursesDTO coursesDTO = new CoursesDTO();
    coursesDTO.setCourses(ImmutableList.of());
    PaginationDTO pagination1 = new PaginationDTO();
    pagination1.setCurrentPage(1);
    pagination1.setTotalPages(1);
    coursesDTO.setPagination(pagination1);
    when(this.mockRestTemplate.getForObject(anyString(), any())).thenReturn(coursesDTO);
    this.evolveClient =
        new EvolveClient(
            this.mockRestTemplate,
            this.apiPath,
            this.maxRetries,
            this.retryMinBackoffSecs,
            this.retryMaxBackoffSecs);
  }

  @Test
  public void testGetCoursesWithUpdatedOnToDate() {
    this.initEmptyCoursesData();

    ZonedDateTime updatedOnTo = ZonedDateTime.now();
    CoursesGetRequest coursesGetRequest = new CoursesGetRequest("en", updatedOnTo);
    long count = this.evolveClient.getCourses(coursesGetRequest).count();

    assertEquals(0, count);

    verify(this.mockRestTemplate, times(1)).getForObject(urlCaptor.capture(), any());

    assertEquals(
        this.apiPath
            + String.format(
                "courses?locale=en&is_active=true&updated_on_to=%s&page=1",
                UriComponentsBuilder.fromPath(updatedOnTo.toString()).toUriString()),
        urlCaptor.getValue());
  }

  @Test
  public void testGetCoursesWithZeroTotalPages() {
    CoursesDTO coursesDTO = new CoursesDTO();
    coursesDTO.setCourses(ImmutableList.of());
    PaginationDTO pagination1 = new PaginationDTO();
    pagination1.setCurrentPage(1);
    pagination1.setTotalPages(0);
    coursesDTO.setPagination(pagination1);
    when(this.mockRestTemplate.getForObject(anyString(), any())).thenReturn(coursesDTO);
    this.evolveClient =
        new EvolveClient(
            this.mockRestTemplate,
            this.apiPath,
            this.maxRetries,
            this.retryMinBackoffSecs,
            this.retryMaxBackoffSecs);

    CoursesGetRequest coursesGetRequest = new CoursesGetRequest("en", null);

    long count = this.evolveClient.getCourses(coursesGetRequest).count();

    assertEquals(0, count);
  }

  @Test
  public void testGetCoursesWhenThrowingException() {
    when(this.mockRestTemplate.getForObject(anyString(), any()))
        .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)));

    this.evolveClient =
        new EvolveClient(
            this.mockRestTemplate,
            this.apiPath,
            this.maxRetries,
            this.retryMinBackoffSecs,
            this.retryMaxBackoffSecs);

    CoursesGetRequest coursesGetRequest = new CoursesGetRequest("en", null);

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> this.evolveClient.getCourses(coursesGetRequest).count());
    assertTrue(exception.getCause() instanceof HttpClientErrorException);
  }

  private void initDataWithInitialFailure() {
    reset(this.mockRestTemplate);
    CourseDTO courseDTO = new CourseDTO();
    courseDTO.setId(1);
    courseDTO.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO.setType("CourseEvolve");

    CoursesDTO coursesDTO = new CoursesDTO();
    coursesDTO.setCourses(ImmutableList.of(courseDTO));
    PaginationDTO pagination1 = new PaginationDTO();
    pagination1.setCurrentPage(1);
    pagination1.setTotalPages(1);
    coursesDTO.setPagination(pagination1);
    when(this.mockRestTemplate.getForObject(anyString(), any()))
        .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)))
        .thenReturn(coursesDTO);
    this.evolveClient =
        new EvolveClient(
            this.mockRestTemplate,
            this.apiPath,
            this.maxRetries,
            this.retryMinBackoffSecs,
            this.retryMaxBackoffSecs);
  }

  @Test
  public void testGetCoursesSucceedsAfterRetry() {
    this.initDataWithInitialFailure();

    this.evolveClient =
        new EvolveClient(
            this.mockRestTemplate,
            this.apiPath,
            this.maxRetries,
            this.retryMinBackoffSecs,
            this.retryMaxBackoffSecs);

    CoursesGetRequest coursesGetRequest = new CoursesGetRequest("en", null);
    long count = this.evolveClient.getCourses(coursesGetRequest).count();

    assertEquals(1, count);
    verify(this.mockRestTemplate, times(2)).getForObject(anyString(), any());
  }

  private void initCourseTranslationData() {
    Mockito.reset(this.mockRestTemplate);
    when(this.mockRestTemplate.postForObject(anyString(), any(), any())).thenReturn("content");
    this.evolveClient =
        new EvolveClient(
            this.mockRestTemplate,
            this.apiPath,
            this.maxRetries,
            this.retryMinBackoffSecs,
            this.retryMaxBackoffSecs);
  }

  @Test
  public void testStartCourseTranslation() {
    this.initCourseTranslationData();

    String response = this.evolveClient.startCourseTranslation(1, "es", Sets.newHashSet());

    verify(this.mockRestTemplate, times(1))
        .postForObject(this.urlCaptor.capture(), this.httpEntityCaptor.capture(), any());

    assertEquals(this.apiPath + "course_translations/1?target_locale=es", urlCaptor.getValue());
    HttpEntity<Object> httpEntity = this.httpEntityCaptor.getValue();
    assertTrue(httpEntity.getHeaders().getAccept().contains(MediaType.APPLICATION_XML));
    assertEquals("content", response);

    this.initCourseTranslationData();

    this.evolveClient.startCourseTranslation(1, "es", Sets.newHashSet("fr", "it"));

    verify(this.mockRestTemplate, times(1))
        .postForObject(this.urlCaptor.capture(), any(HttpEntity.class), any());

    assertEquals(
        this.apiPath
            + "course_translations/1?target_locale=es&additional_locales[]=fr&additional_locales[]=it",
        urlCaptor.getValue());
  }

  @Test
  public void testUpdateCourse() {
    this.evolveClient =
        new EvolveClient(
            this.mockRestTemplate,
            this.apiPath,
            this.maxRetries,
            this.retryMinBackoffSecs,
            this.retryMaxBackoffSecs);

    ZonedDateTime modifiedSince = ZonedDateTime.now();
    this.evolveClient.updateCourse(1, IN_TRANSLATION, modifiedSince);

    verify(this.mockRestTemplate)
        .put(
            this.urlCaptor.capture(),
            this.httpEntityCaptor.capture(),
            this.courseIdCaptor.capture());

    assertEquals(this.apiPath + "courses/{courseId}", this.urlCaptor.getValue());
    assertEquals(1, (int) courseIdCaptor.getValue());
    HttpEntity<Object> httpEntity = this.httpEntityCaptor.getValue();
    HttpHeaders headers = httpEntity.getHeaders();
    assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
    assertEquals(modifiedSince.toEpochSecond() * 1000, headers.getIfModifiedSince());
    assertTrue(httpEntity.getBody() instanceof Map);
    Map<?, ?> body = (Map<?, ?>) httpEntity.getBody();
    assertTrue(body.get("course") instanceof Map);
    assertEquals(((Map<?, ?>) body.get("course")).get("custom_j"), IN_TRANSLATION.getName());
  }

  @Test
  public void testUpdateCourseTranslation() {
    this.evolveClient =
        new EvolveClient(
            this.mockRestTemplate,
            this.apiPath,
            this.maxRetries,
            this.retryMinBackoffSecs,
            this.retryMaxBackoffSecs);

    this.evolveClient.updateCourseTranslation(1, "content");

    verify(this.mockRestTemplate, times(1))
        .put(
            this.urlCaptor.capture(),
            this.httpEntityCaptor.capture(),
            this.courseIdCaptor.capture());

    assertEquals(this.apiPath + "course_translations/{courseId}", this.urlCaptor.getValue());
    HttpEntity<Object> httpEntity = this.httpEntityCaptor.getValue();
    assertEquals(
        MediaType.valueOf("application/xml;charset=UTF-8"),
        httpEntity.getHeaders().getContentType());
    assertEquals("content", httpEntity.getBody());
    assertEquals(1, (int) courseIdCaptor.getValue());
  }

  @Test
  public void testSyncEvolve() {
    Map<String, String> params = ImmutableMap.of("status", "ready");
    when(this.mockRestTemplate.postForObject(anyString(), any(), any(), anyInt()))
        .thenReturn(params);
    this.evolveClient =
        new EvolveClient(
            this.mockRestTemplate,
            this.apiPath,
            this.maxRetries,
            this.retryMinBackoffSecs,
            this.retryMaxBackoffSecs);

    Map<?, ?> response = this.evolveClient.syncEvolve(1);

    verify(this.mockRestTemplate)
        .postForObject(this.urlCaptor.capture(), any(), any(), this.courseIdCaptor.capture());

    assertEquals(
        this.apiPath + "course_translations/{courseId}/evolve_sync", this.urlCaptor.getValue());
    assertEquals(1, (int) courseIdCaptor.getValue());
    assertTrue(response.containsKey("status"));
    assertEquals("ready", response.get("status"));
  }
}
