package com.box.l10n.mojito.service.sla;

import com.box.l10n.mojito.utils.DateTimeUtils;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author jeanaurambault
 */
@Component
public class DropScheduleService {

    static Logger logger = LoggerFactory.getLogger(DropScheduleService.class);

    @Autowired
    DropScheduleConfig dropScheduleConfig;

    @Autowired
    DateTimeUtils dateTimeUtils;

    public DateTime getLastDropCreatedDate() {
        DateTime now = dateTimeUtils.now(dropScheduleConfig.getTimezone());
        DateTime lastDropDueDate = getLastDropDueDate(now);
        return getDropCreatedDate(lastDropDueDate);
    }
    
    DateTime getDropCreatedDate(DateTime dropDueDate) {

        DateTime dropCreatedDate = dropDueDate.withTime(dropScheduleConfig.getCreatedLocalTime());

        Integer dropDueDateDay = dropDueDate.getDayOfWeek();
        Integer dropStartDateDay = getDueDayToStartDay().get(dropDueDateDay);

        dropCreatedDate = dropCreatedDate.withDayOfWeek(dropStartDateDay);

        if (dropStartDateDay > dropDueDateDay) {
            dropCreatedDate = dropCreatedDate.minusWeeks(1);
        }

        return dropCreatedDate;
    }

    DateTime getLastDropDueDate(DateTime before) {

        DateTime lastDropDueDate = null;

        HashSet<Integer> dropDueDaysSet = Sets.newHashSet(dropScheduleConfig.getDueDays());

        for (int daysToSubstract = 0; daysToSubstract <= 7; daysToSubstract++) {
            DateTime candidate = before.minusDays(daysToSubstract).withTime(dropScheduleConfig.getDueLocalTime());

            if (dropDueDaysSet.contains(candidate.getDayOfWeek()) && !candidate.isAfter(before)) {
                lastDropDueDate = candidate;
                break;
            }
        }

        return lastDropDueDate;
    }

    Map<Integer, Integer> getDueDayToStartDay() {
        Map<Integer, Integer> dueDayToStartDay = new HashMap<>();

        for (int i = 0; i < dropScheduleConfig.getDueDays().size(); i++) {
            dueDayToStartDay.put(dropScheduleConfig.getDueDays().get(i), dropScheduleConfig.getCreatedDays().get(i));
        }

        return dueDayToStartDay;
    }

}
