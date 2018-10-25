package com.box.l10n.mojito.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/**
 *
 * @author jeanaurambault
 */
@Component
public class HttpClientErrorExceptionHelper {

    public HttpClientErrorJson toHttpClientErrorJson(HttpClientErrorException hcee) {
        try {
            return new ObjectMapper().readValue(hcee.getResponseBodyAsString(), HttpClientErrorJson.class);
        } catch (IOException ioe) {
            throw new RuntimeException("Can't convert HttpClientErrorException to Json", ioe);
        }
    }
}
