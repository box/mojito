package com.box.l10n.mojito.service.branch.notification;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BranchNotificationMessageSendersTest extends ServiceTestBase {

  @Autowired BranchNotificationMessageSenders branchNotificationMessageSenders;

  @Test
  public void noop() throws Exception {
    BranchNotificationMessageSender noop = branchNotificationMessageSenders.getById("noop-1");
    Assertions.assertThat(noop);
  }
}
