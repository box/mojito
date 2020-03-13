package com.box.l10n.mojito.rest.client;


import com.box.l10n.mojito.rest.entity.GitBlameWithUsage;
import com.box.l10n.mojito.rest.entity.PollableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author emagalindan
 */
@Component
public class GitBlameWithUsageClient extends BaseClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(GitBlameWithUsageClient.class);

    @Override
    public String getEntityName() {
        return "textunits";
    }

    public List<GitBlameWithUsage> getGitBlameWithUsages(Long repositoryId, Integer offset, Integer batchSize) {
        logger.debug("getGitBlameWithUsages");
        Map<String, String> filterParams = new HashMap<>();

        filterParams.put("repositoryIds[]", repositoryId.toString());
        filterParams.put("offset", offset.toString());
        filterParams.put("limit", batchSize.toString());

        return authenticatedRestTemplate.getForObjectAsListWithQueryStringParams(
                getBasePathForEntity() + "/gitBlameWithUsages",
                GitBlameWithUsage[].class,
                filterParams);
    }

    public PollableTask saveGitBlameWithUsages(List<GitBlameWithUsage> gitBlameWithUsages) {
        logger.debug("saveGitBlameWithUsages");
        return authenticatedRestTemplate.postForObject(getBasePathForEntity() + "/gitBlameWithUsagesBatch",
                gitBlameWithUsages, PollableTask.class
        );

    }
}
