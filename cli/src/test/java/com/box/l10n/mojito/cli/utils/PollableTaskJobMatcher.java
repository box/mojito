package com.box.l10n.mojito.cli.utils;

import static com.box.l10n.mojito.quartz.QuartzConfig.DYNAMIC_GROUP_NAME;

import com.box.l10n.mojito.quartz.QuartzPollableJob;
import org.quartz.JobKey;
import org.quartz.Matcher;

public class PollableTaskJobMatcher<T extends QuartzPollableJob<?, ?>> implements Matcher<JobKey> {

  private final Class<T> target;

  public PollableTaskJobMatcher(Class<T> target) {
    this.target = target;
  }

  @Override
  public boolean isMatch(JobKey key) {
    return key.getName().startsWith(target.getName()) && DYNAMIC_GROUP_NAME.equals(key.getGroup());
  }
}
