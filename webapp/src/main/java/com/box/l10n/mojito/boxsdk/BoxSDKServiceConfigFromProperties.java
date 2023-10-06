package com.box.l10n.mojito.boxsdk;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Contains the configuration to connect to the Box Account.
 *
 * @author wyau
 */
@Component
@ConfigurationProperties(prefix = "l10n.boxclient")
public class BoxSDKServiceConfigFromProperties implements BoxSDKServiceConfig {

    /**
     * Use configuration from properties to configure the Box SDK client (else
     * read configuration from the database).
     */
    boolean useConfigsFromProperties = false;
    String clientId;
    String clientSecret;
    String publicKeyId;
    String privateKey;
    String privateKeyPassword;
    String enterpriseId;
    String appUserId;
    String proxyHost;
    Integer proxyPort;
    String proxyUser;
    String proxyPassword;

    /**
     * The folder ID of the Box root folder, for the active profile. It is
     * different from the "All The files" folder because we don't want to
     * pollute the real root folder. Instead all new folders will be created
     * inside this folder.
     * <p>
     * The rootFolderId must be accessible by the user (owned or shared)
     */
    String rootFolderId;

    String dropsFolderId;

    public boolean isUseConfigsFromProperties() {
        return useConfigsFromProperties;
    }

    public void setUseConfigsFromProperties(boolean useConfigsFromProperties) {
        this.useConfigsFromProperties = useConfigsFromProperties;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPrivateKeyPassword() {
        return privateKeyPassword;
    }

    public String getEnterpriseId() {
        return enterpriseId;
    }

    public String getAppUserId() {
        return appUserId;
    }

    public String getRootFolderId() {
        return rootFolderId;
    }

    public String getDropsFolderId() {
        return dropsFolderId;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setPrivateKeyPassword(String privateKeyPassword) {
        this.privateKeyPassword = privateKeyPassword;
    }

    public void setEnterpriseId(String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public void setAppUserId(String appUserId) {
        this.appUserId = appUserId;
    }

    public void setRootFolderId(String rootFolderId) {
        this.rootFolderId = rootFolderId;
    }

    public void setDropsFolderId(String dropsFolderId) {
        this.dropsFolderId = dropsFolderId;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + (this.useConfigsFromProperties ? 1 : 0);
        hash = 41 * hash + Objects.hashCode(this.clientId);
        hash = 41 * hash + Objects.hashCode(this.clientSecret);
        hash = 41 * hash + Objects.hashCode(this.publicKeyId);
        hash = 41 * hash + Objects.hashCode(this.privateKey);
        hash = 41 * hash + Objects.hashCode(this.privateKeyPassword);
        hash = 41 * hash + Objects.hashCode(this.enterpriseId);
        hash = 41 * hash + Objects.hashCode(this.appUserId);
        hash = 41 * hash + Objects.hashCode(this.rootFolderId);
        hash = 41 * hash + Objects.hashCode(this.dropsFolderId);
        hash = 41 * hash + Objects.hashCode(this.proxyHost);
        hash = 41 * hash + Objects.hashCode(this.proxyPort);
        hash = 41 * hash + Objects.hashCode(this.proxyUser);
        hash = 41 * hash + Objects.hashCode(this.proxyPassword);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BoxSDKServiceConfigFromProperties other = (BoxSDKServiceConfigFromProperties) obj;
        if (this.useConfigsFromProperties != other.useConfigsFromProperties) {
            return false;
        }
        if (!Objects.equals(this.clientId, other.clientId)) {
            return false;
        }
        if (!Objects.equals(this.clientSecret, other.clientSecret)) {
            return false;
        }
        if (!Objects.equals(this.publicKeyId, other.publicKeyId)) {
            return false;
        }
        if (!Objects.equals(this.privateKey, other.privateKey)) {
            return false;
        }
        if (!Objects.equals(this.privateKeyPassword, other.privateKeyPassword)) {
            return false;
        }
        if (!Objects.equals(this.enterpriseId, other.enterpriseId)) {
            return false;
        }
        if (!Objects.equals(this.appUserId, other.appUserId)) {
            return false;
        }
        if (!Objects.equals(this.rootFolderId, other.rootFolderId)) {
            return false;
        }
        if (!Objects.equals(this.dropsFolderId, other.dropsFolderId)) {
            return false;
        }
        if (!Objects.equals(this.proxyHost, other.proxyHost)) {
            return false;
        }
        if (!Objects.equals(this.proxyPort, other.proxyPort)) {
            return false;
        }
        if (!Objects.equals(this.proxyUser, other.proxyUser)) {
            return false;
        }
        if (!Objects.equals(this.proxyPassword, other.proxyPassword)) {
            return false;
        }
        return true;
    }

}
