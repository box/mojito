package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotUpdatedException;
import com.box.l10n.mojito.rest.entity.Branch;
import com.box.l10n.mojito.rest.entity.ExportDropConfig;
import com.box.l10n.mojito.rest.entity.ImportRepositoryBody;
import com.box.l10n.mojito.rest.entity.IntegrityChecker;
import com.box.l10n.mojito.rest.entity.Locale;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class QuartzJobsClient extends BaseClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(QuartzJobsClient.class);

    @Override
    public String getEntityName() {
        return "quartz";
    }

    public List<String> getAllDynamicJobs() {
        logger.debug("getAllDynamicJobs");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(getBasePathForEntity()).pathSegment("jobs").path("dynamic");
        return authenticatedRestTemplate.getForObject(uriBuilder.toUriString(), List.class);
    }

    public void deleteAllDynamicJobs() {
        logger.debug("deleteAllDynamicJobs");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(getBasePathForEntity()).pathSegment("jobs").path("dynamic");
        authenticatedRestTemplate.delete(uriBuilder.toUriString());
    }
}
