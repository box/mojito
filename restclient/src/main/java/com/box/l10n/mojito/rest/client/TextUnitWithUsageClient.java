package com.box.l10n.mojito.rest.client;


import com.box.l10n.mojito.rest.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author emagalindan
 */
@Component
public class TextUnitWithUsageClient extends BaseClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TextUnitWithUsageClient.class);

    @Override
    public String getEntityName() {
        return "textunits";
    }

    public List<GitBlameWithUsage> getTextUnitToBlame(Long repositoryId) {

        Map<String, String> filterParams = new HashMap<>();

        filterParams.put("repositoryIds[]", repositoryId.toString());

        return authenticatedRestTemplate.getForObjectAsListWithQueryStringParams(
                getBasePathForEntity() + "/gitBlameWithUsages",
                GitBlameWithUsage[].class,
                filterParams);
    }

    public PollableTask saveGitInfoForTextUnits(List<GitBlameWithUsage> gitInfoForTextUnits) {
        return authenticatedRestTemplate.postForObject(getBasePathForEntity() + "/gitBlameWithUsagesBatch",
                gitInfoForTextUnits, PollableTask.class
        );

    }
}