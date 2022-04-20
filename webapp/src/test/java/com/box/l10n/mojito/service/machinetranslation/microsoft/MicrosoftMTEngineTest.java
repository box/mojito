package com.box.l10n.mojito.service.machinetranslation.microsoft;

import com.box.l10n.mojito.service.machinetranslation.MachineTranslationConfiguration;
import com.box.l10n.mojito.service.machinetranslation.MachineTranslationEngine;
import com.box.l10n.mojito.service.machinetranslation.TranslationDTO;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {
        MicrosoftMTEngineTest.class,
        MicrosoftMTEngineConfiguration.class,
        MachineTranslationConfiguration.class,
        MachineTranslationEngine.class
})
@EnableConfigurationProperties
public class MicrosoftMTEngineTest {
    @Autowired(required = false)
    MicrosoftMTEngine microsoftMTEngine;

    @Autowired(required = false)
    MicrosoftMTEngineConfiguration microsoftMTEngineConfiguration;

    @Test
    public void testTranslate() {
        Assume.assumeNotNull(microsoftMTEngine);

        ImmutableMap<String, ImmutableList<TranslationDTO>> translationsBySourceText = microsoftMTEngine.getTranslationsBySourceText(
                ImmutableList.of("hello world", "some new text"),
                "en",
                ImmutableList.of("fr-FR", "ja"),
                null,
                null);

        List<String> allTranslatedStrings = translationsBySourceText.values().stream()
                .flatMap(Collection::stream)
                .map(TranslationDTO::getText)
                .collect(Collectors.toList());

        Assert.assertEquals(4, allTranslatedStrings.size());
    }

    @Test(expected = HttpClientErrorException.class)
    public void testBadSourceLocaleThrows() {
        Assume.assumeNotNull(microsoftMTEngine);

        microsoftMTEngine.getTranslationsBySourceText(
                ImmutableList.of("hello world", "some new text"),
                "sourceBadLocale",
                ImmutableList.of("fr", "ja"),
                null,
                null);
    }

    @Test(expected = HttpClientErrorException.class)
    public void testBadTargetLocaleThrows() {
        Assume.assumeNotNull(microsoftMTEngine);

        microsoftMTEngine.getTranslationsBySourceText(
                ImmutableList.of("hello world", "some new text"),
                "en",
                ImmutableList.of("badTargetLocale"),
                null,
                null);
    }
}