package com.box.l10n.mojito.rest.resttemplate.stateless;

import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StatelessAuthConfiguration {

  @Bean
  public TokenSupplier tokenSupplier(ResttemplateConfig config) {
    ResttemplateConfig.StatelessAuthentication.Provider provider =
        config.getStateless().getProvider();
    if (provider == ResttemplateConfig.StatelessAuthentication.Provider.MSAL_BROWSER_CODE) {
      return new MsalAuthCodePkceTokenSupplier(config);
    }
    // default to device code
    return new MsalDeviceCodeTokenSupplier(config);
  }
}
