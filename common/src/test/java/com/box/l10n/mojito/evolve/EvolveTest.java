package com.box.l10n.mojito.evolve;

import com.box.l10n.mojito.test.TestWithEnableAutoConfiguration;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.stream.Stream;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {
        TestWithEnableAutoConfiguration.class,
        EvolveConfiguration.class,
        EvolveConfigurationProperties.class})
public class EvolveTest {
    static Logger logger = LoggerFactory.getLogger(EvolveTest.class);

    @Autowired(required = false)
    Evolve evolve;

    @Value("${test.l10n.evolve.courseid:}")
    String courseId;

    @Test
    public void getCourses() {
        Assume.assumeNotNull(evolve);
        Stream<Course> courses = evolve.getCourses();
        courses.filter(c -> "inTranslation".equals(c.getState())).forEach(c -> logger.info("id: {}, state: {}", c.getId(), c.getState()));
    }

    @Test
    public void getTranslationsByCourseIdAndCreateCourseTranslationsById() {
        Assume.assumeNotNull(evolve);
        String translationsByCourseId = evolve.getTranslationsByCourseId(courseId);
        evolve.createCourseTranslationsById(courseId, translationsByCourseId, "fr-FR", false);
    }

    @Test
    public void addVersionAndRtlAttributeToTranslations() throws IOException {

        String withoutAttribute = "{\n" +
                "  \"_translations\": {\n" +
                "     \"5e3e1805bb23502ffd28796c\": {\n" +
                "      \"_id\": \"5e3e1805bb23502ffd28796c\"\n" +
                "    }\n" +
                "  }\n" +
                "}\n";

        String wihtAttribute = new Evolve(null).addVersionAndRtlAttributeToTranslations(withoutAttribute, "fr-FR", false);
        Assert.assertEquals("{\"_translations\":{\"5e3e1805bb23502ffd28796c\":{\"_id\":\"5e3e1805bb23502ffd28796c\"},\"_version\":\"fr-FR\",\"_rtl\":false}}", wihtAttribute);
    }
}
