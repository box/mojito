package com.box.l10n.mojito.boxsdk;

import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.EncryptionAlgorithm;
import com.box.sdk.JWTEncryptionPreferences;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
public class BoxSDKJWTProvider {

  /**
   * Gets the JWT Encryption to be used by the {@link BoxDeveloperEditionAPIConnection}
   *
   * @return
   * @throws BoxSDKServiceException
   */
  public JWTEncryptionPreferences getJWTEncryptionPreferences(
      BoxSDKServiceConfig boxSDKServiceConfig) {
    return getJWTEncryptionPreferences(
        boxSDKServiceConfig.getPublicKeyId(),
        boxSDKServiceConfig.getPrivateKey(),
        boxSDKServiceConfig.getPrivateKeyPassword());
  }

  /**
   * Gets the JWT Encryption to be used by the {@link BoxDeveloperEditionAPIConnection}
   *
   * @return
   * @throws BoxSDKServiceException
   */
  public JWTEncryptionPreferences getJWTEncryptionPreferences(
      String publicKeyId, String privateKey, String privateKeyPassword) {
    JWTEncryptionPreferences encryptionPref = new JWTEncryptionPreferences();
    encryptionPref.setPublicKeyID(publicKeyId);
    encryptionPref.setPrivateKey(privateKey);
    encryptionPref.setPrivateKeyPassword(privateKeyPassword);
    encryptionPref.setEncryptionAlgorithm(EncryptionAlgorithm.RSA_SHA_256);

    return encryptionPref;
  }
}
