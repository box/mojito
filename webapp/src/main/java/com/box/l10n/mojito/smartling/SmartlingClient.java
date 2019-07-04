package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.smartling.response.Errors;
import com.box.l10n.mojito.smartling.response.SourceStringsResponse;
import com.box.l10n.mojito.smartling.response.SourceStrings;
import com.box.l10n.mojito.smartling.response.StringInfo;
import com.box.l10n.mojito.utils.PageFetcherSplitIterator;
import com.ibm.icu.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SmartlingClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);

    static final String API_PULL_SOURCE_STRINGS = "strings-api/v2/projects/{projectId}/source-strings?fileUri={fileUri}&offset={offset}&offset={limit}";

    static final String API_SUCCESS_CODE = "SUCCESS";

    static final int LIMIT = 500;

    OAuth2RestTemplate oAuth2RestTemplate;

    public SmartlingClient(OAuth2RestTemplate oAuth2RestTemplate) {
        this.oAuth2RestTemplate = oAuth2RestTemplate;
    }

    public Stream<StringInfo> getStringInfos(String projectId, String fileUri) {
        PageFetcherSplitIterator<StringInfo> stringInfoPageFetcherSplitIterator = new PageFetcherSplitIterator<StringInfo>((offset, limit) -> {
            SourceStrings sourceStrings = getSourceStrings(
                    projectId,
                    fileUri,
                    offset,
                    limit);

            return sourceStrings.getItems();
        }, LIMIT);

        return StreamSupport.stream(stringInfoPageFetcherSplitIterator, false);
    }

    SourceStrings getSourceStrings(String projectId, String fileUri, Integer offset, Integer limit) throws SmartlingClientException {

        SourceStringsResponse sourceStringsResponse = oAuth2RestTemplate.getForObject(
                API_PULL_SOURCE_STRINGS,
                SourceStringsResponse.class,
                projectId, fileUri, offset, limit);

        if (!API_SUCCESS_CODE.equals(sourceStringsResponse.getCode())) {
            throw new SmartlingClientException("Can't get source strings:" + sourceStringsResponse.getCode());
        }

        return sourceStringsResponse.getData();
    }
}
