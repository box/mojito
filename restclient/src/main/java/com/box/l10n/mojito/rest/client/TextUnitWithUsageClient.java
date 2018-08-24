package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotUpdatedException;
import com.box.l10n.mojito.rest.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        return "textUnitToBlame";
    }

    public List<TextUnitWithUsage> getTextUnitToBlame(Long repositoryId) {

        Map<String, String> filterParams = new HashMap<>();

        filterParams.put("repositoryId", repositoryId.toString());

        return authenticatedRestTemplate.getForObjectAsListWithQueryStringParams(
                getBasePathForEntity(),
                TextUnitWithUsage[].class,
                filterParams);
    }
}
