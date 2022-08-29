package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.VirtualAssetTextUnit;
import com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplate;
import com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplateTest;
import com.google.common.base.Strings;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@Configuration
@SpringBootTest(
    classes = {
      AuthenticatedRestTemplateTest.class,
      RepositoryClient.class,
      VirtualAssetClient.class,
      PollableTaskClient.class
    })
@EnableConfigurationProperties
@Ignore
public class VirtualAssetPerformanceTest {

  static final int NUMBER_OF_TEXTUNITS = 25000;
  /** logger */
  static Logger logger = LoggerFactory.getLogger(VirtualAssetPerformanceTest.class);

  @Autowired RepositoryClient repositoryClient;

  @Autowired VirtualAssetClient virtualAssetClient;

  @Autowired PollableTaskClient pollableTaskClient;

  @Autowired AuthenticatedRestTemplate authenticatedRestTemplate;

  @Test
  public void performance() throws RepositoryNotFoundException {
    //      dmojito repo-create -n perftest3 -d "" -l  "ar-SA" "zh-CN" "zh-TW" "cs-CZ" "da-DK"
    // "de-DE" "el-GR" "en-GB" "es-AR" "es-MX" "es-ES" "fi-FI" "fr-FR" "hi-IN" "hu-HU" "id-ID"
    // "it-IT" "ja-JP" "ko-KR" "ms-MY" "nb-NO" "nl-NL" "pl-PL" "pt-BR" "pt-PT" "ro-RO" "ru-RU"
    // "sk-SK" "sv-SE" "th-TH" "tl-PH" "tr-TR" "uk-UA" "vi-VN"
    String repoName = "perftest3";

    Repository repository = repositoryClient.getRepositoryByName(repoName);

    VirtualAsset v = new VirtualAsset();
    v.setPath("default");
    v.setRepositoryId(repository.getId());
    v.setDeleted(false);
    VirtualAsset virtualAsset = virtualAssetClient.createOrUpdate(v);

    logger.debug("virtual asset id: {}", virtualAsset.getId());

    DateTime start = DateTime.now();
    //        createSourceStrings(virtualAsset);
    //        importTranslations(repository, virtualAsset);

    pullSourceString(virtualAsset, repository);

    pullTranslations(virtualAsset, repository);
    logger.debug("total: {}", PeriodFormat.getDefault().print(new Period(start, DateTime.now())));
  }

  private void pullSourceString(VirtualAsset virtualAsset, Repository repository) {
    repository.getRepositoryLocales().stream()
        .filter(rl -> rl.getParentLocale() == null)
        .forEach(
            rl -> {
              logger.debug("root locale: {}", rl.getLocale().getBcp47Tag());
              long start = System.currentTimeMillis();
              List<VirtualAssetTextUnit> virtualAssetTextUnits =
                  virtualAssetClient.getLocalizedTextUnits(
                      virtualAsset.getId(), rl.getLocale().getId(), "REMOVE_UNTRANSLATED");
              long end = System.currentTimeMillis();
              logger.debug(
                  "file generation: {}", PeriodFormat.getDefault().print(new Period(start, end)));

              //                    ObjectMapper objectMapper = new ObjectMapper();
              //                    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
              //
              // logger.debug(objectMapper.writeValueAsStringUnchecked(virtualAssetTextUnits));
            });
  }

  void pullTranslations(VirtualAsset virtualAsset, Repository repository) {
    logger.debug("pull translations");

    repository.getRepositoryLocales().stream()
        .sorted(Comparator.comparing(rl -> rl.getLocale().getBcp47Tag()))
        .filter(rl -> rl.getParentLocale() != null)
        .forEach(
            rl -> {
              logger.debug("localized locale: {}", rl.getLocale().getBcp47Tag());
              long start = System.currentTimeMillis();
              List<VirtualAssetTextUnit> virtualAssetTextUnits =
                  virtualAssetClient.getLocalizedTextUnits(
                      virtualAsset.getId(), rl.getLocale().getId(), "REMOVE_UNTRANSLATED");
              long end = System.currentTimeMillis();
              logger.debug(
                  "file generation: {}", PeriodFormat.getDefault().print(new Period(start, end)));

              //                    ObjectMapper objectMapper = new ObjectMapper();
              //                    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
              //
              // logger.debug(objectMapper.writeValueAsStringUnchecked(virtualAssetTextUnits));
            });
  }

  void createSourceStrings(VirtualAsset virtualAsset) {
    logger.debug("create the source strings");
    List<VirtualAssetTextUnit> virtualAssetTextUnits =
        IntStream.range(0, NUMBER_OF_TEXTUNITS)
            .mapToObj(
                idx -> {
                  VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
                  virtualAssetTextUnit.setName("name-" + idx);
                  virtualAssetTextUnit.setContent(
                      "content-" + idx + "-" + Strings.padStart("", 30, 'a'));
                  virtualAssetTextUnit.setComment(
                      "comment-" + idx + "-" + Strings.padStart("", 50, 'a'));

                  return virtualAssetTextUnit;
                })
            .collect(Collectors.toList());

    PollableTask pollableTask =
        virtualAssetClient.repalceTextUnits(virtualAsset.getId(), virtualAssetTextUnits);
    pollableTaskClient.waitForPollableTask(pollableTask.getId());
    pollableTask = pollableTaskClient.getPollableTask(pollableTask.getId());
    logger.debug("create source strings: {}", getElapsedTime(pollableTask));
  }

  void importTranslations(Repository repository, VirtualAsset virtualAsset) {
    logger.debug("Import translations");

    repository.getRepositoryLocales().stream()
        .sorted(Comparator.comparing(rl -> rl.getLocale().getBcp47Tag()))
        .filter(rl -> rl.getParentLocale() != null)
        .forEach(
            rl -> {
              logger.debug("import locale: {}", rl.getLocale().getBcp47Tag());
              List<VirtualAssetTextUnit> toImport =
                  IntStream.range(0, NUMBER_OF_TEXTUNITS)
                      .mapToObj(
                          idx -> {
                            VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
                            virtualAssetTextUnit.setName("name-" + idx);
                            virtualAssetTextUnit.setContent(
                                "content-"
                                    + rl.getLocale().getBcp47Tag()
                                    + "-"
                                    + idx
                                    + "-"
                                    + Strings.padStart("", 30, 'a'));
                            return virtualAssetTextUnit;
                          })
                      .collect(Collectors.toList());

              PollableTask pollableTask =
                  virtualAssetClient.importTextUnits(
                      virtualAsset.getId(), rl.getLocale().getId(), toImport);
              pollableTaskClient.waitForPollableTask(pollableTask.getId());
              pollableTask = pollableTaskClient.getPollableTask(pollableTask.getId());
              logger.debug(
                  "import {}: {}", rl.getLocale().getBcp47Tag(), getElapsedTime(pollableTask));
            });
  }

  String getElapsedTime(PollableTask pollableTask) {
    Period period = new Period(pollableTask.getCreatedDate(), pollableTask.getFinishedDate());
    return PeriodFormat.getDefault().print(period);
  }
}
