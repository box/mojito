package com.box.l10n.mojito.smartling;

import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Smartling api uses userIdentifier and userSecret rather than OAuth spec
 */
public class SmartlingRequestInterceptor implements ClientHttpRequestInterceptor {

    private String matchParam(String bodyString, Pattern pattern) {
        Matcher usernameMatcher = pattern.matcher(bodyString);
        String returnString = "";
        if (usernameMatcher.find()) {
            returnString = usernameMatcher.group(1).split("&")[0];
        }
        return returnString;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String bodyString = new String(body);

        Pattern usernamePattern = Pattern.compile("username=(.*)&");
        String username = matchParam(bodyString, usernamePattern);

        Pattern passwordPattern = Pattern.compile("&password=(.*)");
        String password = matchParam(bodyString, passwordPattern);

        String newBody = "{\"userIdentifier\": \"".concat(username).concat("\", \"userSecret\": \"").concat(password).concat("\"}");
        return execution.execute(request, newBody.getBytes());
    }

    public class CustomHttpRequestWrapper extends HttpRequestWrapper {

        public CustomHttpRequestWrapper(HttpRequest request) {
            super(request);
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        }
    }
}
