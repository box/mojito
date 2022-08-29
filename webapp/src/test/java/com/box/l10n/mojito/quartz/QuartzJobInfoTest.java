package com.box.l10n.mojito.quartz;

import org.junit.Assert;
import org.junit.Test;

public class QuartzJobInfoTest {

  @Test
  public void testBuilder() {
    QuartzJobInfo.Builder<String, Void> builder = QuartzJobInfo.newBuilder(TestJob.class);
    QuartzJobInfo<String, Void> quartzJobInfo =
        builder.withExpectedSubTaskNumber(0).withInlineInput(false).withMessage("coucou").build();
    Assert.assertEquals(TestJob.class, quartzJobInfo.getClazz());
  }

  static class TestJob extends QuartzPollableJob<String, Void> {
    @Override
    public Void call(String input) throws Exception {
      logger.debug("do nothing, test");
      return null;
    }
  }
}
