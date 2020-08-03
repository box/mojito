package com.box.l10n.mojito.service.thirdparty.smartling;

import org.junit.Test;

import java.util.regex.Pattern;

import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils.getOutputSourceFile;
import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils.getOutputTargetFile;
import static org.assertj.core.api.Assertions.assertThat;

public class SmartlingFileUtilsTest {

    @Test
    public void testGetFilePattern() {

        Pattern webappmigration = SmartlingFileUtils.getFilePattern("repositoryName");

        assertThat(webappmigration.matcher("repositoryName/0000_singular_source.xml").matches()).isTrue();
        assertThat(webappmigration.matcher("repositoryName/0000_plural_source.xml").matches()).isTrue();
        assertThat(webappmigration.matcher("someotherrepo/0000_plural_source.xml").matches()).isFalse();
        assertThat(webappmigration.matcher("something").matches()).isFalse();
    }

    @Test
    public void testGetOutputSourceFile() {
        assertThat(getOutputSourceFile(1, "repo-test", "singular")).isEqualTo("repo-test/00001_singular_source.xml");
        assertThat(getOutputSourceFile(21, "repo-test", "singular")).isEqualTo("repo-test/00021_singular_source.xml");
        assertThat(getOutputSourceFile(321, "repo-test", "singular")).isEqualTo("repo-test/00321_singular_source.xml");
        assertThat(getOutputSourceFile(4321, "repo-test", "singular")).isEqualTo("repo-test/04321_singular_source.xml");
        assertThat(getOutputSourceFile(54321, "repo-test", "singular")).isEqualTo("repo-test/54321_singular_source.xml");
        assertThat(getOutputSourceFile(99999, "repo-test", "plural")).isEqualTo("repo-test/99999_plural_source.xml");
    }

    @Test
    public void testGetTargetSourceFile() {
        assertThat(getOutputTargetFile(2, "repo-test", "singular", "es-CL")).isEqualTo("repo-test/00002_singular_es-CL.xml");
        assertThat(getOutputTargetFile(32, "repo-test", "singular", "en-US")).isEqualTo("repo-test/00032_singular_en-US.xml");
        assertThat(getOutputTargetFile(432, "repo-test", "singular", "en-US")).isEqualTo("repo-test/00432_singular_en-US.xml");
        assertThat(getOutputTargetFile(5432, "repo-test", "singular", "en-US")).isEqualTo("repo-test/05432_singular_en-US.xml");
        assertThat(getOutputTargetFile(65432, "repo-test", "singular", "en-US")).isEqualTo("repo-test/65432_singular_en-US.xml");
        assertThat(getOutputTargetFile(99999, "repo", "plural", "es-MX")).isEqualTo("repo/99999_plural_es-MX.xml");
    }

}
