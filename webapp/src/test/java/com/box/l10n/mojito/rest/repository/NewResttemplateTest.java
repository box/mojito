package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.rest.resttemplate.FormLoginConfig;
import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import com.ibm.icu.impl.duration.DurationFormatter;
import com.ibm.icu.text.DurationFormat;
import com.ibm.icu.text.MeasureFormat;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;
import org.apache.http.HttpRequest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.joda.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class NewResttemplateTest {

    // test basic auth
    // test basic re-auth
    // test auth failure (wrong password, etc)
    // retry on 503? (how many)

    BasicCookieStore cookieStore = new BasicCookieStore();


    @Test
    public void test2() throws IOException {
        // we're not getting 401 depending on the accept header basiclly
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().setDefaultCookieStore(new BasicCookieStore()).disableRedirectHandling();
        CloseableHttpClient client = httpClientBuilder.build();
        HttpPost httpPost = new HttpPost("http://localhost:8080/api/repositories");
        CloseableHttpResponse execute = client.execute(httpPost);
        System.out.println(execute.getStatusLine().getStatusCode());
        Arrays.stream(execute.getAllHeaders()).forEach(System.out::println);
    }

    @Test
    public void test() {
        HttpClient hc = HttpClientBuilder
                .create()
                .setDefaultCookieStore(cookieStore)
                // TODO(jean) review
                // we have to turn off auto redirect in the rest template because
                // when session expires, it will return a 302 and resttemplate
                // will automatically redirect to /login even before returning
                // the ClientHttpResponse in the interceptor

                // unauthenticated POST lead to 302 instead of 401
//                .disableRedirectHandling()
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(hc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        // the string http message converter leads to 302 on post while we want to get 401...
        // restTemplate.getMessageConverters().stream().forEach(c -> System.out.println(c.getSupportedMediaTypes() + " - " + c.getClass()));
//        restTemplate.setMessageConverters(Arrays.asList(
//                new MappingJackson2HttpMessageConverter()));

        CsrfClientHttpRequestInitializer csrfClientHttpRequestInitializer = new CsrfClientHttpRequestInitializer();
        restTemplate.getClientHttpRequestInitializers().add(csrfClientHttpRequestInitializer);

        // all post will need the CSRF - we could had by making the request + exchange
        restTemplate.getInterceptors().add((request, body, execution) -> {
            System.out.println(request.getMethod());
            ClientHttpResponse execute = execution.execute(request, body);
            System.out.println(execute.getStatusCode());
            if (execute.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // TODO(jean) must limit
                System.out.println("Got unauthorized - try to authenticate");
                authenticate(restTemplate, csrfClientHttpRequestInitializer);
                execute = execution.execute(request, body);
            }
            return execute;
        });

//        authenticate(restTemplate, csrfClientHttpRequestInitializer);


//        ResponseEntity<String> post3 = restTemplate.postForEntity("http://localhost:8080/api/repositories", "{\n" +
//                "  \"name\" : \"mytest" + UUID.randomUUID() + "\"\n" +
//                "}", String.class);
//

        // we want everything to be application/json for api call but not authentication. what spring boot does is
        // in theory nice but has side effects with the 302

        RequestEntity<String> pr = RequestEntity
                .post(URI.create("http://localhost:8080/api/repositories"))
                // those seem useless since they get overriden .. then was it set?
                .accept(MediaType.APPLICATION_JSON)
                .body("{\n" +
                        "  \"name\" : \"mytest" + UUID.randomUUID() + "\"\n" +
                        "}");

        ResponseEntity<String> post3 = restTemplate.exchange(pr, String.class);
        System.out.println(post3);

        if (post3.getStatusCode().is3xxRedirection()) {
            return;
        }

        ResponseEntity<String> repositoriesEntity = restTemplate.getForEntity("http://localhost:8080/api/repositories", String.class);
        System.out.println(repositoriesEntity);

        System.out.println("clear cookies 1");
        cookieStore.clear();

        ResponseEntity<String> repositoriesEntity2 = restTemplate.getForEntity("http://localhost:8080/api/repositories", String.class);
        System.out.println(repositoriesEntity2);

        repositoriesEntity2 = restTemplate.getForEntity("http://localhost:8080/api/repositories", String.class);
        System.out.println(repositoriesEntity2);

        ResponseEntity<String> post1 = restTemplate.postForEntity("http://localhost:8080/api/repositories", "{\n" +
                "  \"name\" : \"mytest" + UUID.randomUUID() + "\"\n" +
                "}", String.class);
        System.out.println(post1);

        System.out.println("clear cookies 2");
        cookieStore.clear();

        // 302 on POST but not on get?
        ResponseEntity<String> post2 = restTemplate.postForEntity("http://localhost:8080/api/repositories", "{\n" +
                "  \"name\" : \"mytest" + UUID.randomUUID() + "\"\n" +
                "}", String.class);
        System.out.println(post2);

    }


    ClientHttpRequestInterceptor clientHttpRequestInterceptor;

    static class CsrfClientHttpRequestInitializer implements ClientHttpRequestInitializer {
        String csrfToken;

        public String getCsrfToken() {
            return csrfToken;
        }

        public void setCsrfToken(String csrfToken) {
            this.csrfToken = csrfToken;
        }

        @Override
        public void initialize(ClientHttpRequest request) {
            // TODO(jean) this is not applied -- get overriden by spring somehow?
            request.getHeaders().add("Accept", MediaType.APPLICATION_JSON.toString());

            if (csrfToken != null) {
                request.getHeaders().add("X-CSRF-TOKEN", csrfToken);
            }
            System.out.println(request.getHeaders());
        }
    }

    ResttemplateConfig resttemplateConfig = new ResttemplateConfig();
    FormLoginConfig formLoginConfig = new FormLoginConfig();

    /**
     * will mutate the cookie cookie store
     * sh
     *
     * @param restTemplate
     * @param csrfClientHttpRequestInitializer
     */
    private void authenticate(RestTemplate restTemplate, CsrfClientHttpRequestInitializer csrfClientHttpRequestInitializer) {

        // get the intial CSRF to authenticate only
        // here we need to resolve http plain ... after that we don't want else it throws 302 -- maybe related to the
        ResponseEntity<String> loginPageResponse = restTemplate.getForEntity("http://localhost:8080/login", String.class);
        Matcher matcher = Pattern.compile("CSRF_TOKEN = '(.*?)';").matcher(loginPageResponse.getBody());

        // only for authentication - to touch the interceptor at this point
        String csrfToken;
        if (matcher.find()) {
            csrfToken = matcher.group(1);
        } else {
            throw new RuntimeException("Can't find CSRF in login page");
        }

        System.out.println(csrfToken);
        displaySessionCookie(cookieStore);

        // Authenticate with the first CSRF and username/password
        // need set headers
        MultiValueMap<String, Object> loginPostParams = new LinkedMultiValueMap<>();
        loginPostParams.add("username", "admin");
        loginPostParams.add("password", "ChangeMe--");

        csrfClientHttpRequestInitializer.setCsrfToken(csrfToken);
        RequestEntity<MultiValueMap<String, Object>> authenticationRequest = RequestEntity
                .post(URI.create("http://localhost:8080/login"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(loginPostParams);

        // need to fetch the new CSRF after loggin, SESSION ID has changed too
        ResponseEntity<String> postLoginResponseEntity = restTemplate.exchange(authenticationRequest, String.class);

        URI locationURI = URI.create(postLoginResponseEntity.getHeaders().get("Location").get(0));
        String expectedLocation = resttemplateConfig.getContextPath() + "/" + formLoginConfig.getLoginRedirectPath();

        // either we keep that are we just try to re-authenticate with a limit - the code being just simpler
        if (postLoginResponseEntity.getStatusCode().equals(HttpStatus.FOUND)
                && expectedLocation.equals(locationURI.getPath())) {

            if (!resttemplateConfig.isCsrfDisable()) {
                ResponseEntity<String> newCsrfEntity = restTemplate.getForEntity("http://localhost:8080/api/csrf-token", String.class);
                csrfClientHttpRequestInitializer.setCsrfToken(newCsrfEntity.getBody());
            }
        } else {
            throw new SessionAuthenticationException("Authentication failed.  Post login status code = " + postLoginResponseEntity.getStatusCode()
                    + ", location = [" + locationURI.getPath() + "], expected location = [" + expectedLocation + "]");
        }


        System.out.println(csrfToken);
        displaySessionCookie(cookieStore);

        //         TODO(jean) that will remove other interceptor, we want to update instead
//        clientHttpRequestInterceptor = new ClientHttpRequestInterceptor() {
//            @Override
//            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
//                request.getHeaders().add("X-CSRF-TOKEN", csrfToken);
//                return execution.execute(request, body);
//            }
//        };
//        restTemplate.setInterceptors(Arrays.asList(clientHttpRequestInterceptor));

    }

    private void displaySessionCookie(CookieStore cookieStore) {
        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("SESSION")) {
                String cookieValue = cookie.getValue();
                System.out.println(cookieValue);
            }
        }
    }

    private void changeSessionCookie(CookieStore cookieStore) {
        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("SESSION")) {
                String cookieValue = cookie.getValue();
                System.out.println(cookieValue);
            }
        }
    }
}
