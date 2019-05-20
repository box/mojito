package com.box.l10n.mojito.smartling;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Smartling api uses userIdentifier and userSecret rather than OAuth spec
 */
public class SmartlingRequestInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String bodyString = new String(body);
        // pull the userIdentifier and userSecret here, currently in the request body as username & password

        execution.execute(request, bodyString.getBytes());
    }
}
