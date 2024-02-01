package com.box.l10n.mojito.service.boxsdk;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.boxsdk.BoxSDKJWTProvider;
import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.BoxUser;
import com.box.sdk.CreateUserParams;
import com.box.sdk.InMemoryLRUAccessTokenCache;
import com.box.sdk.JWTEncryptionPreferences;
import com.ibm.icu.text.MessageFormat;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
public class BoxSDKAppUserService {

  private static final int UNLIMITED_SPACE = -1;
  private static final String MOJITO_APP_USER_NAME = "Mojito System User";

  /** logger */
  static Logger logger = getLogger(BoxSDKAppUserService.class);

  @Autowired BoxSDKJWTProvider boxSDKJWTProvider;

  /**
   * Create a Box App User
   *
   * @return
   * @throws BoxSDKServiceException
   */
  public BoxUser.Info createAppUser(
      String clientId,
      String clientSecret,
      String publicKeyId,
      String privateKey,
      String privateKeyPassword,
      String enterpriseId)
      throws BoxSDKServiceException {

    try {
      logger.debug("Creating Box App User: {}", MOJITO_APP_USER_NAME);
      JWTEncryptionPreferences jwtEncryptionPreferences =
          boxSDKJWTProvider.getJWTEncryptionPreferences(
              publicKeyId, privateKey, privateKeyPassword);
      BoxDeveloperEditionAPIConnection appEnterpriseConnection =
          BoxDeveloperEditionAPIConnection.getAppEnterpriseConnection(
              enterpriseId,
              clientId,
              clientSecret,
              jwtEncryptionPreferences,
              new InMemoryLRUAccessTokenCache(5));

      CreateUserParams createUserParams = new CreateUserParams();
      createUserParams.setSpaceAmount(UNLIMITED_SPACE);

      return BoxUser.createAppUser(appEnterpriseConnection, MOJITO_APP_USER_NAME, createUserParams);
    } catch (BoxAPIException e) {
      String msg = MessageFormat.format("Couldn't create App User: {0}", e.getResponse());
      logger.error(msg);
      throw new BoxSDKServiceException(msg, e);
    }
  }
}
