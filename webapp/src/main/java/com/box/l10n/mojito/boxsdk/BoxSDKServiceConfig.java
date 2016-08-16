package com.box.l10n.mojito.boxsdk;

/**
 * @author wyau
 */
public interface BoxSDKServiceConfig {
    /**
     * @return The Client ID of the API Key
     */
    String getClientId();

    /**
     * @return The Client Secret of the API Key
     */
    String getClientSecret();

    /**
     * @return The Public Key ID submitted to Box
     * See: https://box-content.readme.io/v2.0/docs/app-auth#section-submitting-the-public-key
     */
    String getPublicKeyId();

    /**
     * @return The Private Key that corresponds to the Public Key uploaded.
     * See: https://box-content.readme.io/v2.0/docs/app-auth#section-submitting-the-public-key
     */
    String getPrivateKey();

    /**
     * @return The password for the Private Key if any
     */
    String getPrivateKeyPassword();

    /**
     * @return The Enterprise ID granted access to the API key
     */
    String getEnterpriseId();

    /**
     * @return The App User that has access to the {@link #getRootFolderId()} and {@link #getDropsFolderId()}
     */
    String getAppUserId();

    /**
     * @return The folder ID of the Box root folder, for the active profile. It is
     * different from the "All The files" folder because we don't want to
     * pollute the real root folder. Instead all new folders will be created
     * inside this folder.
     * <p>
     * The rootFolderId must be accessible by the user (owned or shared)
     */
    String getRootFolderId();

    /**
     * @return The folder ID to contain all the drops
     */
    String getDropsFolderId();

}
