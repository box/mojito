package com.box.l10n.mojito.react;

import com.box.l10n.mojito.security.SecurityConfig;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ReactSecurityConfig {

    String unauthRedirectTo;

    Map<String, OAuth2> oAuth2;

    boolean handleRedirectAsErrorOnImageUpload;

    @Autowired
    public ReactSecurityConfig(SecurityConfig securityConfig) {
        Preconditions.checkNotNull(securityConfig);
        this.unauthRedirectTo = securityConfig.getUnauthRedirectTo();
        this.oAuth2 = securityConfig.getoAuth2().entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey(),
                e -> new OAuth2(e.getValue())
        ));
        this.handleRedirectAsErrorOnImageUpload = securityConfig.isHandleRedirectAsErrorOnImageUpload();
    }

    public boolean isHandleRedirectAsErrorOnImageUpload() {
        return handleRedirectAsErrorOnImageUpload;
    }

    public void setHandleRedirectAsErrorOnImageUpload(boolean handleRedirectAsErrorOnImageUpload) {
        this.handleRedirectAsErrorOnImageUpload = handleRedirectAsErrorOnImageUpload;
    }

    public String getUnauthRedirectTo() {
        return unauthRedirectTo;
    }

    public void setUnauthRedirectTo(String unauthRedirectTo) {
        this.unauthRedirectTo = unauthRedirectTo;
    }

    public Map<String, OAuth2> getoAuth2() {
        return oAuth2;
    }

    public void setoAuth2(Map<String, OAuth2> oAuth2) {
        this.oAuth2 = oAuth2;
    }

    public static class OAuth2 {
        String uiLabelText;

        public OAuth2(SecurityConfig.OAuth2 oAuth2) {
            uiLabelText = oAuth2.getUiLabelText();
        }

        public String getUiLabelText() {
            return uiLabelText;
        }

        public void setUiLabelText(String uiLabelText) {
            this.uiLabelText = uiLabelText;
        }
    }
}
