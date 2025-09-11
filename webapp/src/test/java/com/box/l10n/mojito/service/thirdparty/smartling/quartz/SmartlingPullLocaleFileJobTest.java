package com.box.l10n.mojito.service.thirdparty.smartling.quartz;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
import static com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService.IntegrityChecksType.fromLegacy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.ThirdPartyFileChecksum;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.thirdparty.ThirdPartyFileChecksumRepository;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.SmartlingClientException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

public class SmartlingPullLocaleFileJobTest {

  @Mock SmartlingClient smartlingClientMock;

  @Mock TextUnitBatchImporterService textUnitBatchImporterServiceMock;

  @Mock ThirdPartyFileChecksumRepository thirdPartyFileChecksumRepositoryMock;

  @Mock RepositoryRepository repositoryRepositoryMock;

  @Mock LocaleRepository localeRepositoryMock;

  @Captor ArgumentCaptor<List<TextUnitDTO>> textUnitListCaptor;

  SmartlingPullLocaleFileJob smartlingPullLocaleFileJob;

  SmartlingPullLocaleFileJobInput smartlingPullLocaleFileJobInput;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    doReturn(null)
        .when(smartlingClientMock)
        .uploadFile(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString());
    doReturn(null)
        .when(smartlingClientMock)
        .uploadLocalizedFile(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString());
    when(textUnitBatchImporterServiceMock.importTextUnits(
            anyList(),
            eq(fromLegacy(false, true)),
            eq(TextUnitBatchImporterService.ImportMode.ALWAYS_IMPORT)))
        .thenReturn(null);

    RetryBackoffSpec retryConfiguration =
        Retry.backoff(10, Duration.ofMillis(1)).maxBackoff(Duration.ofMillis(10));
    when(smartlingClientMock.getRetryConfiguration()).thenReturn(retryConfiguration);

    smartlingPullLocaleFileJobInput = new SmartlingPullLocaleFileJobInput();
    smartlingPullLocaleFileJobInput.setRepositoryName("testRepo");
    smartlingPullLocaleFileJobInput.setLocaleBcp47Tag("fr-CA");
    smartlingPullLocaleFileJobInput.setSmartlingLocale("fr-CA");
    smartlingPullLocaleFileJobInput.setFileName("testFile");
    smartlingPullLocaleFileJobInput.setDeltaPull(false);
    smartlingPullLocaleFileJobInput.setDryRun(false);
    smartlingPullLocaleFileJobInput.setSmartlingFilePrefix("singular");
    smartlingPullLocaleFileJobInput.setSmartlingProjectId("testProjectId");
    smartlingPullLocaleFileJobInput.setLocaleId(1);
    smartlingPullLocaleFileJobInput.setSchedulerName(DEFAULT_SCHEDULER_NAME);
    smartlingPullLocaleFileJobInput.setPluralSeparator(" _");

    smartlingPullLocaleFileJob = new SmartlingPullLocaleFileJob();
    smartlingPullLocaleFileJob.smartlingClient = smartlingClientMock;
    smartlingPullLocaleFileJob.repositoryRepository = repositoryRepositoryMock;
    smartlingPullLocaleFileJob.localeRepository = localeRepositoryMock;
    smartlingPullLocaleFileJob.textUnitBatchImporterService = textUnitBatchImporterServiceMock;
    smartlingPullLocaleFileJob.thirdPartyFileChecksumRepository =
        thirdPartyFileChecksumRepositoryMock;
    smartlingPullLocaleFileJob.meterRegistry = new SimpleMeterRegistry();
  }

  @Test
  public void testPullSingular() throws Exception {

    String pullResponse =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n"
            + "<!--comment 1-->\n"
            + "<string name=\"src/main/res/values/strings.xml#@#hello\" tmTextUnitId=\"1\">Hello in fr-CA</string>\n"
            + "<!--comment 2-->\n"
            + "<string name=\"src/main/res/values/strings.xml#@#bye\" tmTextUnitId=\"2\">Bye in fr-CA</string>\n"
            + "</resources>\n";

    doReturn(pullResponse)
        .when(smartlingClientMock)
        .downloadPublishedFile(eq("testProjectId"), eq("fr-CA"), eq("testFile"), eq(false));

    smartlingPullLocaleFileJob.call(smartlingPullLocaleFileJobInput);

    verify(textUnitBatchImporterServiceMock, times(1))
        .importTextUnits(
            textUnitListCaptor.capture(),
            eq(fromLegacy(false, true)),
            eq(TextUnitBatchImporterService.ImportMode.ALWAYS_IMPORT));

    List<TextUnitDTO> captured = textUnitListCaptor.getValue();

    assertThat(captured).hasSize(2);
    assertThat(captured)
        .extracting(
            "name",
            "comment",
            "target",
            "assetPath",
            "targetLocale",
            "repositoryName",
            "pluralForm")
        .containsExactlyInAnyOrder(
            tuple(
                "hello",
                "comment 1",
                "Hello in fr-CA",
                "src/main/res/values/strings.xml",
                "fr-CA",
                "testRepo",
                null),
            tuple(
                "bye",
                "comment 2",
                "Bye in fr-CA",
                "src/main/res/values/strings.xml",
                "fr-CA",
                "testRepo",
                null));
  }

  @Test
  public void testPullPlural() throws Exception {

    smartlingPullLocaleFileJobInput.setSmartlingFilePrefix("plural");

    String pullResponse =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n"
            + "<plurals name=\"src/main/res/values/strings.xml#@#plural_things\">\n"
            + "<item quantity=\"one\">One thing in fr-CA</item>\n"
            + "<item quantity=\"few\">Few things in fr-CA</item>\n"
            + "<item quantity=\"other\">Other things in fr-CA</item>\n"
            + "</plurals>\n"
            + "</resources>\n";

    doReturn(pullResponse)
        .when(smartlingClientMock)
        .downloadPublishedFile(eq("testProjectId"), eq("fr-CA"), eq("testFile"), eq(false));

    smartlingPullLocaleFileJob.call(smartlingPullLocaleFileJobInput);

    verify(textUnitBatchImporterServiceMock, times(1))
        .importTextUnits(
            textUnitListCaptor.capture(),
            eq(fromLegacy(false, true)),
            eq(TextUnitBatchImporterService.ImportMode.ALWAYS_IMPORT));

    List<TextUnitDTO> captured = textUnitListCaptor.getValue();

    assertThat(captured).hasSize(3);
    assertThat(captured)
        .extracting(
            "name",
            "comment",
            "target",
            "assetPath",
            "targetLocale",
            "repositoryName",
            "pluralForm")
        .containsExactlyInAnyOrder(
            tuple(
                "plural_things _one",
                null,
                "One thing in fr-CA",
                "src/main/res/values/strings.xml",
                "fr-CA",
                "testRepo",
                "one"),
            tuple(
                "plural_things _few",
                null,
                "Few things in fr-CA",
                "src/main/res/values/strings.xml",
                "fr-CA",
                "testRepo",
                "few"),
            tuple(
                "plural_things _other",
                null,
                "Other things in fr-CA",
                "src/main/res/values/strings.xml",
                "fr-CA",
                "testRepo",
                "other"));
  }

  @Test
  public void testPullPluralsFix() throws Exception {

    smartlingPullLocaleFileJobInput.setPluralFixForLocale(true);
    smartlingPullLocaleFileJobInput.setLocaleBcp47Tag("ja-JP");
    smartlingPullLocaleFileJobInput.setSmartlingLocale("ja-JP");
    smartlingPullLocaleFileJobInput.setSmartlingFilePrefix("plural");

    String pullResponse =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n"
            + "<plurals name=\"src/main/res/values/strings.xml#@#plural_things\">\n"
            + "<item quantity=\"one\">One thing in ja-JP</item>\n"
            + "<item quantity=\"few\">Few things in ja-JP</item>\n"
            + "<item quantity=\"other\">Other things in ja-JP</item>\n"
            + "</plurals>\n"
            + "</resources>\n";

    doReturn(pullResponse)
        .when(smartlingClientMock)
        .downloadPublishedFile(eq("testProjectId"), eq("ja-JP"), eq("testFile"), eq(false));

    smartlingPullLocaleFileJob.call(smartlingPullLocaleFileJobInput);

    verify(textUnitBatchImporterServiceMock, times(1))
        .importTextUnits(
            textUnitListCaptor.capture(),
            eq(fromLegacy(false, true)),
            eq(TextUnitBatchImporterService.ImportMode.ALWAYS_IMPORT));

    List<TextUnitDTO> captured = textUnitListCaptor.getValue();

    assertThat(captured).hasSize(4);
    assertThat(captured)
        .extracting(
            "name",
            "comment",
            "target",
            "assetPath",
            "targetLocale",
            "repositoryName",
            "pluralForm")
        .containsExactlyInAnyOrder(
            tuple(
                "plural_things _one",
                null,
                "One thing in ja-JP",
                "src/main/res/values/strings.xml",
                "ja-JP",
                "testRepo",
                "one"),
            tuple(
                "plural_things _few",
                null,
                "Few things in ja-JP",
                "src/main/res/values/strings.xml",
                "ja-JP",
                "testRepo",
                "few"),
            tuple(
                "plural_things _other",
                null,
                "Other things in ja-JP",
                "src/main/res/values/strings.xml",
                "ja-JP",
                "testRepo",
                "other"),
            tuple(
                "plural_things _many",
                null,
                "Other things in ja-JP",
                "src/main/res/values/strings.xml",
                "ja-JP",
                "testRepo",
                "many"));
  }

  @Test
  public void testPullDryRun() throws Exception {

    smartlingPullLocaleFileJobInput.setDryRun(true);
    String pullResponse =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n"
            + "<!--comment 1-->\n"
            + "<string name=\"src/main/res/values/strings.xml#@#hello\" tmTextUnitId=\"1\">Hello in fr-CA</string>\n"
            + "<!--comment 2-->\n"
            + "<string name=\"src/main/res/values/strings.xml#@#bye\" tmTextUnitId=\"2\">Bye in fr-CA</string>\n"
            + "</resources>\n";

    doReturn(pullResponse)
        .when(smartlingClientMock)
        .downloadPublishedFile(eq("testProjectId"), eq("fr-CA"), eq("testFile"), eq(false));

    smartlingPullLocaleFileJob.call(smartlingPullLocaleFileJobInput);

    verify(textUnitBatchImporterServiceMock, times(0))
        .importTextUnits(
            textUnitListCaptor.capture(),
            eq(fromLegacy(false, true)),
            eq(TextUnitBatchImporterService.ImportMode.ALWAYS_IMPORT));
  }

  @Test(expected = SmartlingClientException.class)
  public void testRetriesExhaustedDuringPull() throws Exception {

    doThrow(new SmartlingClientException(new HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT)))
        .when(smartlingClientMock)
        .downloadPublishedFile(anyString(), anyString(), anyString(), anyBoolean());

    smartlingPullLocaleFileJob.call(smartlingPullLocaleFileJobInput);
  }

  @Test
  public void testPullShortCircuitIfNoChanges() throws Exception {

    String pullResponse =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n"
            + "<!--comment 1-->\n"
            + "<string name=\"src/main/res/values/strings.xml#@#hello\" tmTextUnitId=\"1\">Hello in fr-CA</string>\n"
            + "<!--comment 2-->\n"
            + "<string name=\"src/main/res/values/strings.xml#@#bye\" tmTextUnitId=\"2\">Bye in fr-CA</string>\n"
            + "</resources>\n";

    ThirdPartyFileChecksum existingChecksum = new ThirdPartyFileChecksum();
    existingChecksum.setMd5(DigestUtils.md5Hex(pullResponse.getBytes(StandardCharsets.UTF_8)));

    Repository testRepo = new Repository();
    testRepo.setId(1L);
    testRepo.setName("testRepo");

    Locale frCA = new Locale();
    frCA.setBcp47Tag("fr-CA");
    frCA.setId(1L);

    when(repositoryRepositoryMock.findByName(isA(String.class))).thenReturn(testRepo);
    when(localeRepositoryMock.findByBcp47Tag(isA(String.class))).thenReturn(frCA);

    when(thirdPartyFileChecksumRepositoryMock.findByRepositoryAndFileNameAndLocale(
            isA(Repository.class), isA(String.class), isA(Locale.class)))
        .thenReturn(Optional.of(existingChecksum));

    smartlingPullLocaleFileJobInput.setDeltaPull(true);

    doReturn(pullResponse)
        .when(smartlingClientMock)
        .downloadPublishedFile(eq("testProjectId"), eq("fr-CA"), eq("testFile"), eq(false));

    smartlingPullLocaleFileJob.call(smartlingPullLocaleFileJobInput);

    verify(textUnitBatchImporterServiceMock, times(0))
        .importTextUnits(
            textUnitListCaptor.capture(),
            eq(fromLegacy(false, true)),
            eq(TextUnitBatchImporterService.ImportMode.ALWAYS_IMPORT));

    verify(thirdPartyFileChecksumRepositoryMock, times(0)).save(any(ThirdPartyFileChecksum.class));
  }

  @Test
  public void testPullShortCircuitIfChanges() throws Exception {
    ThirdPartyFileChecksum existingChecksum = new ThirdPartyFileChecksum();
    existingChecksum.setMd5("existingChecksum");

    Repository testRepo = new Repository();
    testRepo.setId(1L);
    testRepo.setName("testRepo");

    Locale frCA = new Locale();
    frCA.setBcp47Tag("fr-CA");
    frCA.setId(1L);

    when(repositoryRepositoryMock.findByName(isA(String.class))).thenReturn(testRepo);
    when(localeRepositoryMock.findByBcp47Tag(isA(String.class))).thenReturn(frCA);

    when(thirdPartyFileChecksumRepositoryMock.findByRepositoryAndFileNameAndLocale(
            isA(Repository.class), isA(String.class), isA(Locale.class)))
        .thenReturn(Optional.of(existingChecksum));

    smartlingPullLocaleFileJobInput.setDeltaPull(true);

    String pullResponse =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n"
            + "<!--comment 1-->\n"
            + "<string name=\"src/main/res/values/strings.xml#@#hello\" tmTextUnitId=\"1\">Hello in fr-CA</string>\n"
            + "<!--comment 2-->\n"
            + "<string name=\"src/main/res/values/strings.xml#@#bye\" tmTextUnitId=\"2\">Bye in fr-CA</string>\n"
            + "</resources>\n";

    doReturn(pullResponse)
        .when(smartlingClientMock)
        .downloadPublishedFile(eq("testProjectId"), eq("fr-CA"), eq("testFile"), eq(false));

    smartlingPullLocaleFileJob.call(smartlingPullLocaleFileJobInput);

    verify(textUnitBatchImporterServiceMock, times(1))
        .importTextUnits(
            textUnitListCaptor.capture(),
            eq(fromLegacy(false, true)),
            eq(TextUnitBatchImporterService.ImportMode.ALWAYS_IMPORT));

    verify(thirdPartyFileChecksumRepositoryMock, times(1)).save(any(ThirdPartyFileChecksum.class));
  }
}
