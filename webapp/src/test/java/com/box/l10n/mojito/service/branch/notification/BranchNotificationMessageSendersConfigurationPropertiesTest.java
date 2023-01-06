package com.box.l10n.mojito.service.branch.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSendersConfigurationProperties.NoopConfigurationProperties;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {BranchNotificationMessageSendersConfigurationProperties.class},
    properties = {
      "l10n.branchNotification.notifiers.noop.noop-1.enabled=true",
    })
@EnableConfigurationProperties
public class BranchNotificationMessageSendersConfigurationPropertiesTest {

  @Autowired
  BranchNotificationMessageSendersConfigurationProperties
      branchNotificationMessageSendersConfigurationProperties;

  @Test
  public void noop() {
    final Map<String, NoopConfigurationProperties> noop =
        branchNotificationMessageSendersConfigurationProperties.getNoop();
    assertThat(noop).containsOnlyKeys("noop-1");
    assertThat(noop.get("noop-1").isEnabled()).isTrue();
  }
}
