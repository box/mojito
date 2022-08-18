package com.box.l10n.mojito.service.thirdparty;

import static org.assertj.core.groups.Tuple.tuple;

import com.box.l10n.mojito.rest.thirdparty.ThirdPartySyncAction;
import java.util.Arrays;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {ThirdPartySyncJobsConfigTest.class, ThirdPartySyncJobsConfig.class},
    properties = {
      "l10n.thirdPartySyncJobs.repo1-a.cron=",
      "l10n.thirdPartySyncJobs.repo1-a.repository=repo1",
      "l10n.thirdPartySyncJobs.repo1-a.thirdPartyProjectId=xyz",
      "l10n.thirdPartySyncJobs.repo1-a.actions=PUSH,PULL",
      "l10n.thirdPartySyncJobs.repo1-a.pluralSeparator=",
      "l10n.thirdPartySyncJobs.repo1-a.localeMapping=tr:tr-TR,fr:fr-FR",
      "l10n.thirdPartySyncJobs.repo1-a.skipTextUnitsWithPattern=",
      "l10n.thirdPartySyncJobs.repo1-a.skipAssetsWithPathPattern=",
      "l10n.thirdPartySyncJobs.repo1-a.includeTextUnitsWithPattern=",
      "l10n.thirdPartySyncJobs.repo1-a.options[0]=smartling-placeholder-format=NONE",
      "l10n.thirdPartySyncJobs.repo1-a.options[1]=smartling-placeholder-format-custom=%s",
      "l10n.thirdPartySyncJobs.repo1-b.cron=",
      "l10n.thirdPartySyncJobs.repo1-b.repository=repo1",
      "l10n.thirdPartySyncJobs.repo1-b.thirdPartyProjectId=xyz",
      "l10n.thirdPartySyncJobs.repo1-b.actions=MAP_TEXTUNIT,PUSH_SCREENSHOT",
    })
@EnableConfigurationProperties
public class ThirdPartySyncJobsConfigTest {

  @Autowired ThirdPartySyncJobsConfig thirdPartySyncJobsConfig;

  @Test
  public void testConfig() {
    final Map<String, ThirdPartySyncJobConfig> thirdPartySyncJobs =
        thirdPartySyncJobsConfig.getThirdPartySyncJobs();
    Assertions.assertThat(thirdPartySyncJobs)
        .containsOnlyKeys(
            "repo1-a",
            "repo1-b",
            "testRepository"); // testRepository comes from application-test.properties
    Assertions.assertThat(thirdPartySyncJobs)
        .extractingByKeys("repo1-a", "repo1-b")
        .extracting("repository", "thirdPartyProjectId", "actions", "options")
        .containsExactly(
            tuple(
                "repo1",
                "xyz",
                Arrays.asList(ThirdPartySyncAction.PUSH, ThirdPartySyncAction.PULL),
                Arrays.asList(
                    "smartling-placeholder-format=NONE", "smartling-placeholder-format-custom=%s")),
            tuple(
                "repo1",
                "xyz",
                Arrays.asList(
                    ThirdPartySyncAction.MAP_TEXTUNIT, ThirdPartySyncAction.PUSH_SCREENSHOT),
                null));
  }
}
