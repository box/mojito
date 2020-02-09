package com.box.l10n.mojito.evolve;

import com.box.l10n.mojito.iterators.ListWithLastPage;
import com.box.l10n.mojito.iterators.PageFetcherCurrentAndTotalPagesSplitIterator;
import com.box.l10n.mojito.json.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class Evolve {
    static Logger logger = LoggerFactory.getLogger(Evolve.class);

    RestTemplate restTemplate;

    public Evolve(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Stream<Course> getCourses() {

        PageFetcherCurrentAndTotalPagesSplitIterator<Course> iterator = new PageFetcherCurrentAndTotalPagesSplitIterator<>(
                pageToFetch -> {
                    Courses courses = restTemplate.getForObject("courses?currentPage={currentPage}", Courses.class, pageToFetch);
                    ListWithLastPage<Course> courseListWithLastPage = new ListWithLastPage<>();
                    courseListWithLastPage.setList(courses.getCourses());
                    courseListWithLastPage.setLastPage(courses.getTotalPages());
                    return courseListWithLastPage;
                }, 1);

        return StreamSupport.stream(iterator, false);
    }

    /**
     * Gets the subset of the course needed for translation
     *
     * @param courseId
     * @return
     */
    public String getTranslationsByCourseId(String courseId) {
        return restTemplate.getForObject("translate/{courseId}", String.class, courseId);
    }

    /**
     * Create a translated course.
     *
     * @param courseId
     * @param translatedCourse
     * @param locale
     * @param isRTL
     */
    public void createCourseTranslationsById(String courseId, String translatedCourse, String locale, boolean isRTL) {
        translatedCourse = addVersionAndRtlAttributeToTranslations(translatedCourse, locale, isRTL);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(translatedCourse, headers);
        String response = restTemplate.postForObject("translate/{courseId}", httpEntity, String.class, courseId);
        logger.debug("course created: {}", response);
    }

    String addVersionAndRtlAttributeToTranslations(String translatedCourse, String locale, boolean isRTL) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTreeUnchecked(translatedCourse);
        JsonNode translations = jsonNode.get("_translations");
        if (jsonNode == null || !jsonNode.isObject()) {
            throw new RuntimeException("there must be a _translations object node");
        }
        ObjectNode translationsObjectNode = (ObjectNode) translations;
        translationsObjectNode.put("_version", locale);
        translationsObjectNode.put("_rtl", isRTL);
        return objectMapper.writeValueAsStringUnchecked(jsonNode);
    }
}
