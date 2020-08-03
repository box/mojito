package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingOptions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingOptions.parseList;
import static org.assertj.core.api.Assertions.assertThat;

public class SmartlingOptionsTest {

    List<String> options = new ArrayList<>();
    SmartlingOptions result;

    @Test
    public void testParseOptions() {

        result = parseList(Arrays.asList());

        assertThat(result.getPluralFixForLocales()).isEmpty();
        assertThat(result.isDryRun()).isFalse();
        assertThat(result.getCustomPlaceholderFormat()).isNull();
        assertThat(result.getPlaceholderFormat()).isNull();

        options = Arrays.asList("smartling-plural-fix=es-CL", "option2=value2");
        result = parseList(options);

        assertThat(result.getPluralFixForLocales()).isNotEmpty();
        assertThat(result.getPluralFixForLocales()).containsOnly("es-CL");
        assertThat(result.isDryRun()).isFalse();
        assertThat(result.getCustomPlaceholderFormat()).isNull();
        assertThat(result.getPlaceholderFormat()).isNull();

        options = Arrays.asList(
                "dry-run=something",
                "smartling-placeholder-format=value@of%Option=With.Chars",
                "smartling-placeholder-format-custom=\\{\\{\\}\\}|\\{\\{?.+?\\}\\}?|\\%\\%\\(.+?\\)s|\\%\\(.+?\\)s|\\%\\(.+?\\)d|\\%\\%s|\\%s"
        );
        result = parseList(options);

        assertThat(result.getPluralFixForLocales()).isEmpty();
        assertThat(result.isDryRun()).isFalse();
        assertThat(result.getPlaceholderFormat()).isEqualTo("value@of%Option=With.Chars");
        assertThat(result.getCustomPlaceholderFormat()).isEqualTo("\\{\\{\\}\\}|\\{\\{?.+?\\}\\}?|\\%\\%\\(.+?\\)s|\\%\\(.+?\\)s|\\%\\(.+?\\)d|\\%\\%s|\\%s");

        options = Arrays.asList(
                "dry-run=true",
                "smartling-plural-fix=es-CL,en-US"
        );
        result = parseList(options);

        assertThat(result.getPluralFixForLocales()).isNotEmpty();
        assertThat(result.getPluralFixForLocales()).containsOnly("es-CL", "en-US");
        assertThat(result.isDryRun()).isTrue();
        assertThat(result.getPlaceholderFormat()).isNull();
        assertThat(result.getCustomPlaceholderFormat()).isNull();
    }
}
