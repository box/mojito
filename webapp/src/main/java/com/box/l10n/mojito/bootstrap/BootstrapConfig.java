package com.box.l10n.mojito.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author jaurambault
 */
@Component
@ConfigurationProperties("l10n.bootstrap")
public class BootstrapConfig {

  boolean enabled = true;

  DefaultUser defaultUser = new DefaultUser();

  public static class DefaultUser {

    String username = "admin";

    String password = "ChangeMe";

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public DefaultUser getDefaultUser() {
    return defaultUser;
  }

  public void setDefaultUser(DefaultUser defaultUser) {
    this.defaultUser = defaultUser;
  }
}
