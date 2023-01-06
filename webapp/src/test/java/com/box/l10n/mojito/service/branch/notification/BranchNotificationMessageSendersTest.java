package com.box.l10n.mojito.service.branch.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class BranchNotificationMessageSendersTest {

  @Test
  public void noop() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.noop.noop-1.enabled=true");
    BranchNotificationMessageSenders branchNotificationMessageSenders =
        new BranchNotificationMessageSenders(config, null, null, null);
    assertThat(branchNotificationMessageSenders.getById("noop-1")).isNotNull();
  }

  @Test
  public void noopInvalidEnabled() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.noop.noop-1.enabled=false");

    assertThatThrownBy(() -> new BranchNotificationMessageSenders(config, null, null, null))
        .hasMessage("only enabled=true is accepted value");
  }

  @Test
  public void noopInvalidId() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.noop.badid-1.enabled=true");
    assertThatThrownBy(() -> new BranchNotificationMessageSenders(config, null, null, null))
        .hasMessage("name must start with prefix: noop-");
  }

  private static BranchNotificationMessageSendersConfigurationProperties
      getTestBranchNotificationMessageSendersConfigurationProperties(String... pairs) {
    AnnotationConfigApplicationContext annotationConfigApplicationContext =
        new AnnotationConfigApplicationContext();
    annotationConfigApplicationContext.register(
        BranchNotificationMessageSendersConfigurationProperties.class,
        ConfigurationPropertiesAutoConfiguration.class);
    TestPropertyValues.of(pairs).applyTo(annotationConfigApplicationContext);
    annotationConfigApplicationContext.refresh();
    BranchNotificationMessageSendersConfigurationProperties config =
        annotationConfigApplicationContext.getBean(
            BranchNotificationMessageSendersConfigurationProperties.class);
    return config;
  }
}
