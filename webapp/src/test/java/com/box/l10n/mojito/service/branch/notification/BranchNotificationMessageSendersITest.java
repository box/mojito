package com.box.l10n.mojito.service.branch.notification;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BranchNotificationMessageSendersITest extends ServiceTestBase {

  @Autowired BranchNotificationMessageSenders branchNotificationMessageSenders;

  @Test
  public void noop() throws Exception {
    // noop-1 is the default test notifier, defined in application-test.properties
    BranchNotificationMessageSender noop = branchNotificationMessageSenders.getById("noop-1");
    Assertions.assertThat(noop).isNotNull();
  }
}
