package com.box.l10n.mojito.security;

import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.rest.resttemplate.CredentialProvider;
import com.box.l10n.mojito.rest.resttemplate.FormLoginAuthenticationCsrfTokenInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import java.io.IOException;

/**
 * @author wyau
 */
public class AuthenticationTest extends WSTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AuthenticationTest.class);

    @Autowired
    FormLoginAuthenticationCsrfTokenInterceptor formLoginAuthenticationCsrfTokenInterceptor;

    @Autowired
    CredentialProvider credentialProvider;

    @Before
    public void before() {
        logger.debug("Resetting authenticated session in rest template so the next time will have to re-auth");
        formLoginAuthenticationCsrfTokenInterceptor.resetAuthentication();

        logger.debug("Resetting credential as it might have been polutted in another test");
        formLoginAuthenticationCsrfTokenInterceptor.setCredentialProvider(credentialProvider);
    }

    @Test
    public void testSuccessfulAuth() {
        ResponseEntity<String> result = authenticatedRestTemplate.getForEntity("", String.class);
        Assert.assertEquals(HttpStatus.SC_OK, result.getStatusCode().value());
    }

    @Test
    public void testSuccessfulAuthMultipleRequests() {
        ResponseEntity<String> result = authenticatedRestTemplate.getForEntity("", String.class);
        Assert.assertEquals(HttpStatus.SC_OK, result.getStatusCode().value());

        result = authenticatedRestTemplate.getForEntity("", String.class);
        Assert.assertEquals(HttpStatus.SC_OK, result.getStatusCode().value());
    }

    @Test
    public void testSuccessfulAuthAfterResettingCookieStore() {
        ResponseEntity<String> result = authenticatedRestTemplate.getForEntity("", String.class);
        Assert.assertEquals(HttpStatus.SC_OK, result.getStatusCode().value());

        logger.debug("Muck with cookie store (ie. maybe a session got changed on a prevous random request");
        CookieStore cookieStore = authenticatedRestTemplate.getRestTemplate().getCookieStore();
        cookieStore.clear();

        logger.debug("The subsequent request is still fine");
        result = authenticatedRestTemplate.getForEntity("", String.class);
        Assert.assertEquals(HttpStatus.SC_OK, result.getStatusCode().value());
    }

    @Test(expected = SessionAuthenticationException.class)
    public void testUnsuccessfulAuthWithIncorrectUser() throws IOException {
        formLoginAuthenticationCsrfTokenInterceptor.setCredentialProvider(new CredentialProvider() {
            @Override
            public String getUsername() {
                return "badUser";
            }

            @Override
            public String getPassword() {
                return credentialProvider.getPassword();
            }
        });

        String result = authenticatedRestTemplate.getForObject("", String.class);
    }

    @Test(expected = SessionAuthenticationException.class)
    public void testUnsuccessfulAuthWithIncorrectPassword() throws IOException {
        formLoginAuthenticationCsrfTokenInterceptor.setCredentialProvider(new CredentialProvider() {
            @Override
            public String getUsername() {
                return credentialProvider.getUsername();
            }

            @Override
            public String getPassword() {
                return "bad password";
            }
        });

        String result = authenticatedRestTemplate.getForObject("", String.class);
    }
}
