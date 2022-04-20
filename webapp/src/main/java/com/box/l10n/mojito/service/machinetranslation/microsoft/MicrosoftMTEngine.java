package com.box.l10n.mojito.service.machinetranslation.microsoft;

import com.box.l10n.mojito.service.machinetranslation.MachineTranslationEngine;
import com.box.l10n.mojito.service.machinetranslation.TranslationDTO;
import com.box.l10n.mojito.service.machinetranslation.TranslationSource;
import com.box.l10n.mojito.service.machinetranslation.microsoft.response.MicrosoftTextTranslationDTO;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import io.micrometer.core.annotation.Timed;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Microsoft Machine Translation API client.
 *
 * @author garion
 */
public class MicrosoftMTEngine implements MachineTranslationEngine {
    public static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
    public static final String OCP_APIM_SUBSCRIPTION_REGION = "Ocp-Apim-Subscription-Region";
    public static final String X_CLIENT_TRACE_ID = "X-ClientTraceId";

    static Logger logger = LoggerFactory.getLogger(MicrosoftMTEngine.class);

    private final RestTemplate restTemplate = new RestTemplate();

    private final MicrosoftMTEngineConfiguration mtEngineConfiguration;

    static final String API_TRANSLATE = "translate";
    static final String API_VERSION = "3.0";

    public MicrosoftMTEngine(MicrosoftMTEngineConfiguration mtEngineConfiguration) {
        this.mtEngineConfiguration = mtEngineConfiguration;
    }

    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public TranslationSource getSource() {
        return TranslationSource.MICROSOFT_MT;
    }

    /**
     * Calls Azure Cognitive Services Translator API to do machine translation on a set of strings.
     * See official API documentation at:
     * https://docs.microsoft.com/en-us/azure/cognitive-services/translator/reference/v3-0-translate
     *
     * @param textSources    List of source strings to translate.
     * @param sourceBcp47Tag Specifies the BCP47 language tag of the input text. Find which languages are
     *                       available to translate from by looking up supported languages using the translation
     *                       scope. If the from parameter is not specified, automatic language detection is applied to
     *                       determine the source language.
     * @param sourceMimeType Optional parameter. Defines whether the text being translated is plain
     *                       text or HTML text. Any HTML needs to be a well-formed, complete element. Possible values
     *                       are: plain (default) or html.
     * @param customModel    Optional parameter. A string specifying the category (domain) of the
     *                       translation. This parameter is used to get translations from a customized system built
     *                       with Custom Translator. Add the Category ID from your Custom Translator project details
     *                       to this parameter to use your deployed customized system. Default value is: general.
     * @return
     */
    @Override
    @Timed("MicrosoftMTEngine.translate")
    public ImmutableMap<String, ImmutableList<TranslationDTO>> getTranslationsBySourceText(
            List<String> textSources,
            String sourceBcp47Tag,
            List<String> targetBcp47Tags,
            String sourceMimeType,
            String customModel) {

        HttpHeaders headers = getHttpHeaders();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put("api-version", Collections.singletonList(API_VERSION));
        params.put("to", targetBcp47Tags);

        if (Strings.isNotBlank(sourceBcp47Tag)) {
            params.put("from", Collections.singletonList(sourceBcp47Tag));
        }

        if (Strings.isNotBlank(sourceMimeType)) {
            params.put("textType", Collections.singletonList(sourceMimeType));
        }

        if (Strings.isNotBlank(customModel)) {
            params.put("domain", Collections.singletonList(customModel));
        }

        String finalUri =
                UriComponentsBuilder.fromHttpUrl(getUrl(API_TRANSLATE))
                        .queryParams(params)
                        .build()
                        .toUriString();

        // TODO(garion) enforce API limits and implement batching when limits are exceeded
        // https://docs.microsoft.com/en-us/azure/cognitive-services/translator/request-limits

        List<Map<String, String>> body =
                textSources.stream()
                        .map(text -> Collections.singletonMap("text", text))
                        .collect(Collectors.toList());

        HttpEntity<List<Map<String, String>>> requestEntity = new HttpEntity<>(body, headers);

        try {
            MicrosoftTextTranslationDTO[] response =
                    restTemplate.postForObject(
                            finalUri, requestEntity, MicrosoftTextTranslationDTO[].class);

            return getTranslationsBySourceTextFromResponse(textSources, response, targetBcp47Tags);
        } catch (HttpClientErrorException e) {
            // Log only the request body, as the headers contain authentication information.
            String errorMessage =
                    String.format(
                            "Could not Machine Translate posted to: %s with %s: %s and request body: %s",
                            finalUri,
                            X_CLIENT_TRACE_ID,
                            requestEntity.getHeaders().get(X_CLIENT_TRACE_ID),
                            body);
            logger.error(errorMessage, e);
            throw e;
        }
    }

    private ImmutableMap<String, ImmutableList<TranslationDTO>> getTranslationsBySourceTextFromResponse(
            List<String> textSources, MicrosoftTextTranslationDTO[] response, List<String> targetBcp47Tags) {

        // Translation objects in the output for each source string are returned in the same order as provided by the client.
        return Streams.zip(textSources.stream(), Arrays.stream(response),
                        (sourceText, microsoftTextTranslationDTO) -> new AbstractMap.SimpleEntry<>(
                                sourceText,
                                getTranslationDTOS(targetBcp47Tags, microsoftTextTranslationDTO)))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private ImmutableList<TranslationDTO> getTranslationDTOS(List<String> targetBcp47Tags, MicrosoftTextTranslationDTO microsoftTextTranslationDTO) {
        // Within a set of translations for a specific source string, the languages are ordered in the same way as
        // the order of target languages provided to the API.
        return Streams.zip(targetBcp47Tags.stream(), microsoftTextTranslationDTO.getTranslations().stream(),
                        (requestTargetBcp47Tag, microsoftLanguageTranslationDTO) -> {
                            TranslationDTO translation = new TranslationDTO();
                            translation.setTranslationSource(getSource());
                            translation.setBcp47Tag(requestTargetBcp47Tag);
                            translation.setText(microsoftLanguageTranslationDTO.getText());
                            return translation;
                        })
                .collect(ImmutableList.toImmutableList());
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add(OCP_APIM_SUBSCRIPTION_KEY, mtEngineConfiguration.getSubscriptionKey());
        headers.add(OCP_APIM_SUBSCRIPTION_REGION, mtEngineConfiguration.getSubscriptionRegion());
        headers.add(X_CLIENT_TRACE_ID, String.valueOf(UUID.randomUUID()));
        return headers;
    }

    private String getUrl(String subpath) {
        return mtEngineConfiguration.getBaseApiUrl() + subpath;
    }
}
