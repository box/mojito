package com.box.l10n.mojito.service;

import com.box.l10n.mojito.io.Files;
import com.google.common.base.Stopwatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.stream.Collectors;

public class NormalizationUtilsTest {

    static Logger logger = LoggerFactory.getLogger(NormalizationUtilsTest.class);

    @Ignore
    @Test
    public void NormalizationICUvsJDK() {
        Files.find(Paths.get("pathtopofiles"),
                100,
                (p, f) -> p.toString().endsWith("messages.po"))
                .forEach(path -> {
                    String content = Files.lines(path).collect(Collectors.joining());

                    Stopwatch stopwatchICU = Stopwatch.createStarted();
                    String normalize = NormalizationUtils.normalize(content);
                    stopwatchICU.stop();

                    Stopwatch stopwatchJDK = Stopwatch.createStarted();
                    // JDK can get very slow for langauge like hi-IN (ICU: PT0.043746286S , JDK: PT27.087855617S) and bn_IN (ICU: PT0.08280765S , JDK: PT3M52.119210107S)
                    String normalize2 = Normalizer.normalize(content, Normalizer.Form.NFC);
                    stopwatchJDK.stop();

                    logger.info("{} --> ICU: {} , JDK: {} ", path, stopwatchICU.elapsed(), stopwatchJDK.elapsed());

                    if (!normalize.equals(normalize2)) {
                        logger.error("different content for: {}", path);
                    }
                });
    }
}