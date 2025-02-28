package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.apiclient.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.apiclient.model.PollableTask;
import com.box.l10n.mojito.apiclient.model.RepositoryRepository;
import com.box.l10n.mojito.apiclient.model.VirtualAsset;
import com.box.l10n.mojito.apiclient.model.VirtualAssetTextUnit;
import com.box.l10n.mojito.resttemplate.AuthenticatedRestTemplate;
import com.box.l10n.mojito.resttemplate.AuthenticatedRestTemplateTest;
import com.google.common.base.Strings;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
      VirtualAssetWsApi.class,
      PollableTaskClient.class
    })
@EnableConfigurationProperties
@Ignore
public class VirtualAssetPerformanceTest {

  static final int NUMBER_OF_TEXTUNITS = 25000;

  /** logger */
  static Logger logger = LoggerFactory.getLogger(VirtualAssetPerformanceTest.class);

  @Autowired RepositoryClient repositoryClient;

  @Autowired VirtualAssetWsApi virtualAssetClient;

  @Autowired PollableTaskClient pollableTaskClient;

  @Autowired AuthenticatedRestTemplate authenticatedRestTemplate;

  @Test
  public void performance() throws RepositoryNotFoundException {
    //      dmojito repo-create -n perftest3 -d "" -l  "ar-SA" "zh-CN" "zh-TW" "cs-CZ" "da-DK"
    // "de-DE" "el-GR" "en-GB" "es-AR" "es-MX" "es-ES" "fi-FI" "fr-FR" "hi-IN" "hu-HU" "id-ID"
    // "it-IT" "ja-JP" "ko-KR" "ms-MY" "nb-NO" "nl-NL" "pl-PL" "pt-BR" "pt-PT" "ro-RO" "ru-RU"
    // "sk-SK" "sv-SE" "th-TH" "tl-PH" "tr-TR" "uk-UA" "vi-VN"
    String repoName = "perftest3";

    RepositoryRepository repository = repositoryClient.getRepositoryByName(repoName);

    VirtualAsset v = new VirtualAsset();
    v.setPath("default");
    v.setRepositoryId(repository.getId());
    v.setDeleted(false);
    VirtualAsset virtualAsset = virtualAssetClient.createOrUpdateVirtualAsset(v);

    logger.debug("virtual asset id: {}", virtualAsset.getId());

    long start = System.currentTimeMillis();
    //        createSourceStrings(virtualAsset);
    //        importTranslations(repository, virtualAsset);

    pullSourceString(virtualAsset, repository);

    pullTranslations(virtualAsset, repository);

    long end = System.currentTimeMillis();

    logger.debug("total: {}", JSR310Migration.toWordBasedDuration(start, end));
  }

  private void pullSourceString(VirtualAsset virtualAsset, RepositoryRepository repository) {
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
              logger.debug("file generation: {}", JSR310Migration.toWordBasedDuration(start, end));

              //                    ObjectMapper objectMapper = new ObjectMapper();
              //                    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
              //
              // logger.debug(objectMapper.writeValueAsStringUnchecked(virtualAssetTextUnits));
            });
  }

  void pullTranslations(VirtualAsset virtualAsset, RepositoryRepository repository) {
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
              logger.debug("file generation: {}", JSR310Migration.toWordBasedDuration(start, end));

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
        virtualAssetClient.replaceTextUnits(virtualAssetTextUnits, virtualAsset.getId());
    pollableTaskClient.waitForPollableTask(pollableTask.getId());
    pollableTask = pollableTaskClient.getPollableTaskById(pollableTask.getId());
    logger.debug("create source strings: {}", getElapsedTime(pollableTask));
  }

  void importTranslations(RepositoryRepository repository, VirtualAsset virtualAsset) {
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
                  virtualAssetClient.importLocalizedTextUnits(
                      toImport, virtualAsset.getId(), rl.getLocale().getId());
              pollableTaskClient.waitForPollableTask(pollableTask.getId());
              pollableTask = pollableTaskClient.getPollableTaskById(pollableTask.getId());
              logger.debug(
                  "import {}: {}", rl.getLocale().getBcp47Tag(), getElapsedTime(pollableTask));
            });
  }

  String getElapsedTime(PollableTask pollableTask) {
    return JSR310Migration.toWordBasedDuration(
        pollableTask.getCreatedDate(), pollableTask.getFinishedDate());
  }
}
