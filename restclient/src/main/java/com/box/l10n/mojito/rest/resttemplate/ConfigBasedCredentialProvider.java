package com.box.l10n.mojito.rest.resttemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This will provide autowired credential values from properties files
 *
 * @author wyau
 */
@Component
public class ConfigBasedCredentialProvider implements CredentialProvider {

  @Autowired ResttemplateConfig resttemplateConfig;

  @Override
  public String getUsername() {
    return resttemplateConfig.getAuthentication().getUsername();
  }

  @Override
  public String getPassword() {
    return resttemplateConfig.getAuthentication().getPassword();
  }
}
