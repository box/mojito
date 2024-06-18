package com.box.l10n.mojito.service.thirdparty.smartling.quartz;

import static com.box.l10n.mojito.service.thirdparty.ThirdPartyTMSUtils.isFileEqualToPreviousRun;

import com.box.l10n.mojito.LocaleMappingHelper;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentMapper;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentReader;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.thirdparty.ThirdPartyFileChecksumRepository;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingPluralFix;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.SmartlingClientException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;
import reactor.core.publisher.Mono;

public class SmartlingPullLocaleFileJob
    extends QuartzPollableJob<SmartlingPullLocaleFileJobInput, Void> {

  static Logger logger = LoggerFactory.getLogger(SmartlingPullLocaleFileJob.class);

  @Autowired MeterRegistry meterRegistry;

  @Autowired ThirdPartyFileChecksumRepository thirdPartyFileChecksumRepository;

  @Autowired TextUnitBatchImporterService textUnitBatchImporterService;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired LocaleRepository localeRepository;

  @Autowired SmartlingClient smartlingClient;

  @Autowired LocaleMappingHelper localeMappingHelper;

  @Override
  public Void call(SmartlingPullLocaleFileJobInput input) throws Exception {
    try (var timer =
        Timer.resource(meterRegistry, "SmartlingSync.processPullBatch")
            .tag("repository", input.getRepositoryName())
            .tag("locale", input.getLocaleBcp47Tag())
            .tag("deltaPull", Boolean.toString(input.isDeltaPull()))) {

      String localeTag = input.getLocaleBcp47Tag();
      String smartlingLocale = input.getSmartlingLocale();
      String fileName = input.getFileName();
      AndroidStringDocumentMapper mapper =
          new AndroidStringDocumentMapper(
              input.getPluralSeparator(), null, localeTag, input.getRepositoryName());

      logger.debug(
          "Download localized file from Smartling for file: {}, Mojito locale: {} and Smartling locale: {}",
          fileName,
          localeTag,
          smartlingLocale);

      String fileContent =
          Mono.fromCallable(
                  () ->
                      smartlingClient.downloadPublishedFile(
                          input.getSmartlingProjectId(), smartlingLocale, fileName, false))
              .retryWhen(
                  smartlingClient
                      .getRetryConfiguration()
                      .doBeforeRetry(
                          e ->
                              logger.info(
                                  String.format(
                                      "Retrying after failure to download file %s", fileName),
                                  e.failure())))
              .doOnError(
                  e -> {
                    String msg =
                        String.format("Error downloading file %s: %s", fileName, e.getMessage());
                    logger.error(msg, e);
                    throw new SmartlingClientException(msg, e);
                  })
              .blockOptional()
              .orElseThrow(
                  () ->
                      new SmartlingClientException(
                          "Error with download from Smartling, file content string is not present."));

      if (input.isDeltaPull()
          && matchesChecksumFromPreviousSync(input, localeTag, fileName, fileContent)) {
        logger.info(
            "Checksum match for "
                + fileName
                + " in locale "
                + localeTag
                + ", skipping text unit import.");
        return null;
      }

      List<TextUnitDTO> textUnits;

      try {
        textUnits = mapper.mapToTextUnits(AndroidStringDocumentReader.fromText(fileContent));
      } catch (ParserConfigurationException | IOException | SAXException e) {
        String msg = "An error occurred when processing a pull batch";
        logger.error(msg, e);
        throw new RuntimeException(msg, e);
      }

      if (!textUnits.isEmpty()
          && input.getSmartlingFilePrefix().equalsIgnoreCase("plural")
          && input.isPluralFixForLocale()) {
        textUnits = SmartlingPluralFix.fixTextUnits(textUnits);
      }

      if (!input.isDryRun()) {
        logger.debug("Importing text units for locale: {}", smartlingLocale);
        textUnitBatchImporterService.importTextUnits(textUnits, false, true);
      }
    }
    return null;
  }

  public boolean matchesChecksumFromPreviousSync(
      SmartlingPullLocaleFileJobInput input,
      String localeTag,
      String fileName,
      String fileContent) {
    return isFileEqualToPreviousRun(
        thirdPartyFileChecksumRepository,
        repositoryRepository.findByName(input.getRepositoryName()),
        localeRepository.findByBcp47Tag(localeTag),
        fileName,
        fileContent,
        meterRegistry);
  }
}
