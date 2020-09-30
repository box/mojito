package com.box.l10n.mojito.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

public class RestTemplateUtils {

    public void enableFeature(RestTemplate restTemplate, DeserializationFeature feature) {
        restTemplate.getMessageConverters().stream().
                filter(httpMessageConverter -> httpMessageConverter instanceof MappingJackson2HttpMessageConverter).
                map(httpMessageConverter -> (MappingJackson2HttpMessageConverter) httpMessageConverter).
                map(MappingJackson2HttpMessageConverter::getObjectMapper).forEach(objectMapper -> {
            objectMapper.enable(feature);
        });
    }
}
