package com.box.l10n.mojito.security;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Contains the security configuration.
 *
 * @author jaurambault
 */
@Component
@ConfigurationProperties(prefix = "l10n.security")
public class SecurityConfig {

  /**
   * To activate different authentcation types.
   *
   * <p>Multiple authentication can work together eg. use "OAUTH2,DATABASE" to both allow OAuth2 and
   * database authentication.
   */
  List<AuthenticationType> authenticationType = Arrays.asList(AuthenticationType.DATABASE);

  /**
   * URL to redirect to when missing authentication. If not specify, goes to the login page.
   *
   * <p>This can be use to redirect to a principal OAuth provider and skip the Mojito's login page.
   * The login page is still accessible if accessed directly.
   */
  String unauthRedirectTo;

  Map<String, OAuth2> oAuth2 = new HashMap<>();

  public List<AuthenticationType> getAuthenticationType() {
    return authenticationType;
  }

  public void setAuthenticationType(List<AuthenticationType> authenticationType) {
    this.authenticationType = authenticationType;
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

  /** Types of authentication available */
  public enum AuthenticationType {
    LDAP,
    DATABASE,
    AD,
    HEADER,
    OAUTH2,
    JWT
  }

  public static class OAuth2 {
    String uiLabelText;

    String unwrapUserAttributes;

    /**
     * In case the principal ID form the auth provider is a UUID, it is of little use on the Mojito
     * side for tasks such as sending notifications, associating with Git commit, PRs, etc.
     *
     * <p>With this option, the username is extracted from the email. This assumes a 1:1 mapping
     * between the email and the Unix username
     *
     * <p>Currently only implement in {@link UserDetailImplOidcUserService}
     */
    boolean usernameFromEmail = false;

    String givenNameAttribute = "first_name";
    String surnameAttribute = "last_name";
    String commonNameAttribute = "name";

    public String getUiLabelText() {
      return uiLabelText;
    }

    public void setUiLabelText(String uiLabelText) {
      this.uiLabelText = uiLabelText;
    }

    public String getGivenNameAttribute() {
      return givenNameAttribute;
    }

    public void setGivenNameAttribute(String givenNameAttribute) {
      this.givenNameAttribute = givenNameAttribute;
    }

    public String getSurnameAttribute() {
      return surnameAttribute;
    }

    public void setSurnameAttribute(String surnameAttribute) {
      this.surnameAttribute = surnameAttribute;
    }

    public String getCommonNameAttribute() {
      return commonNameAttribute;
    }

    public void setCommonNameAttribute(String commonNameAttribute) {
      this.commonNameAttribute = commonNameAttribute;
    }

    public String getUnwrapUserAttributes() {
      return unwrapUserAttributes;
    }

    public void setUnwrapUserAttributes(String unwrapUserAttributes) {
      this.unwrapUserAttributes = unwrapUserAttributes;
    }

    public boolean isUsernameFromEmail() {
      return usernameFromEmail;
    }

    public void setUsernameFromEmail(boolean usernameFromEmail) {
      this.usernameFromEmail = usernameFromEmail;
    }
  }
}
