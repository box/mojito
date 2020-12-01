package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.service.DBUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * This removes asset extraction by branch that have an asset extraction that is used as last asset extraction.
 * <p>
 * That happens for repository that a single branch in the old implementation. If not removed then it leads to the
 * asset being re-processed all the time and potentially if adding branches the state would become corrupted.
 *
 * @author jaurambault
 */
@Profile("!disablescheduling")
@Configuration
@Component
@DisallowConcurrentExecution
@ConditionalOnProperty(value = "l10n.assetextractionbybranch-remover", havingValue = "true")
public class AssetExtractionByBranchRemoverJob implements Job {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AssetExtractionByBranchRemoverJob.class);

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DBUtils dbUtils;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        if (dbUtils.isMysql()) {
            logger.info("For Mysql only, remove asset extraction by branch that have an asset extraction that is used as last asset extraction");
            try {
                int deleteCount = jdbcTemplate.update("delete aebb\n" +
                        "from asset_extraction_by_branch aebb\n" +
                        "inner join asset a on aebb.asset_id = a.id\n" +
                        "where a.last_successful_asset_extraction_id = aebb.asset_extraction_id");
                logger.info("AssetExtractionByBranch delete count: {}", deleteCount);
            } catch (Exception e) {
                logger.error("Couldn't remove asset extraction by branch, ignore", e);
            }
        } else {
            logger.trace("Don't support asset updates if not MySQL");
        }
    }

    @Bean(name = "jobDetailAssetExtractionByBranchRemover")
    JobDetailFactoryBean jobDetailAssetExtractionByBranchRemover() {
        JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
        jobDetailFactory.setJobClass(AssetExtractionByBranchRemoverJob.class);
        jobDetailFactory.setDescription("Remove asset extraction by branch that have an asset extraction that is used as last asset extraction");
        jobDetailFactory.setDurability(true);
        return jobDetailFactory;
    }

    @Bean
    SimpleTriggerFactoryBean triggerAssetExtractionByBranchRemover(@Qualifier("jobDetailAssetExtractionByBranchRemover") JobDetail job) {
        SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
        trigger.setJobDetail(job);
        trigger.setRepeatInterval(Duration.ofMinutes(10).toMillis());
        trigger.setRepeatCount(2);
        return trigger;
    }

}
