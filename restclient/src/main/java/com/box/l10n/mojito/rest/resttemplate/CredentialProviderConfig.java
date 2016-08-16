package com.box.l10n.mojito.rest.resttemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Configures the credential provider
 * 
 * @author jaurambault
 */
@Configuration
public class CredentialProviderConfig {

    @Autowired
    ResttemplateConfig resttemplateConfig;

    @Autowired
    SystemPromptCredentialProvider systemPromptCredentialProvider;

    @Autowired
    ConfigBasedCredentialProvider configBasedCredentialProvider;

    @Bean
    @Primary
    CredentialProvider getCredentialProvider() {

        CredentialProvider credentialProvider;
        
        if (ResttemplateConfig.Authentication.CredentialProvider.CONSOLE.equals(resttemplateConfig.getAuthentication().getCredentialProvider())) {
            credentialProvider = systemPromptCredentialProvider;
        } else {
            credentialProvider = configBasedCredentialProvider;
        }
        
        return credentialProvider;
    }

}
