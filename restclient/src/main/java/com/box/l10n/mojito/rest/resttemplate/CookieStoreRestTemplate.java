package com.box.l10n.mojito.rest.resttemplate;

import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * A Rest Template with {@link HttpComponentsClientHttpRequestFactory} that exposes
 * the cookie store.
 *
 * @author wyau
 */
@Component
public class CookieStoreRestTemplate extends RestTemplate {

    CookieStore cookieStore;

    public CookieStoreRestTemplate() {
        super();
        cookieStore = new BasicCookieStore();
        setCookieStoreAndUpdateRequestFactory(cookieStore);
    }

    public void setCookieStoreAndUpdateRequestFactory(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
        HttpClient hc = HttpClientBuilder
                .create()
                .setDefaultCookieStore(cookieStore)
                // TODO(jean) review
                // we have to turn off auto redirect in the rest template because
                // when session expires, it will return a 302 and resttemplate
                // will automatically redirect to /login even before returning
                // the ClientHttpResponse in the interceptor
                .disableRedirectHandling()
                .build();

        setRequestFactory(new HttpComponentsClientHttpRequestFactory(hc));
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }
}
