package com.box.l10n.mojito.cli.utils;

import com.box.l10n.mojito.quartz.QuartzPollableJob;
import org.quartz.JobKey;
import org.quartz.Matcher;

import static com.box.l10n.mojito.quartz.QuartzConfig.DYNAMIC_GROUP_NAME;

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
