package com.box.l10n.mojito.service.boxsdk;

import com.box.l10n.mojito.boxsdk.BoxSDKJWTProvider;
import com.box.l10n.mojito.boxsdk.BoxSDKServiceException;
import com.box.sdk.*;
import com.ibm.icu.text.MessageFormat;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author wyau
 */
@Component
public class BoxSDKAppUserService {

    private static final int UNLIMITED_SPACE = -1;
    private static final String MOJITO_APP_USER_NAME = "Mojito System User";

    /**
     * logger
     */
    static Logger logger = getLogger(BoxSDKAppUserService.class);

    @Autowired
    BoxSDKJWTProvider boxSDKJWTProvider;

    /**
     * Create a Box App User
     *
     * @return
     * @throws BoxSDKServiceException
     */
    public BoxUser.Info createAppUser(String clientId, String clientSecret, String publicKeyId,
                String privateKey, String privateKeyPassword, String enterpriseId,
              String proxyHost, Integer proxyPort, String proxyUser, String proxyPassword) throws BoxSDKServiceException {

        try {
            logger.debug("Creating Box App User: {}", MOJITO_APP_USER_NAME);
            JWTEncryptionPreferences jwtEncryptionPreferences = boxSDKJWTProvider.getJWTEncryptionPreferences(publicKeyId, privateKey, privateKeyPassword);
            BoxDeveloperEditionAPIConnection appEnterpriseConnection = new BoxDeveloperEditionAPIConnection(enterpriseId,
                    DeveloperEditionEntityType.ENTERPRISE, clientId, clientSecret, jwtEncryptionPreferences, new InMemoryLRUAccessTokenCache(5));

            if (proxyHost != null && proxyPort != null) {
                logger.debug("Setting proxy for Box API connection");
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                appEnterpriseConnection.setProxy(proxy);

                if (proxyUser != null) {
                    appEnterpriseConnection.setProxyBasicAuthentication(proxyUser, proxyPassword);
                }
            }

            appEnterpriseConnection.authenticate();

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
