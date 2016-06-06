package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.boxsdk.BoxSDKServiceConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "box_sdk_service_config")
public class BoxSDKServiceConfigEntity extends AuditableEntity implements BoxSDKServiceConfig {
    @Column(name = "client_id")
    private String clientId;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "client_secret")
    private String clientSecret;

    @Column(name = "public_key_id")
    private String publicKeyId;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "private_key", length = Integer.MAX_VALUE)
    private String privateKey;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "private_key_password")
    private String privateKeyPassword;

    @Column(name = "enterprise_id")
    private String enterpriseId;

    @Column(name = "app_user_id")
    private String appUserId;

    @Column(name = "root_folder_id")
    private String rootFolderId;

    @Column(name = "drops_folder_id")
    private String dropsFolderId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPrivateKeyPassword() {
        return privateKeyPassword;
    }

    public void setPrivateKeyPassword(String privateKeyPassword) {
        this.privateKeyPassword = privateKeyPassword;
    }

    public String getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getAppUserId() {
        return appUserId;
    }

    public void setAppUserId(String appUserId) {
        this.appUserId = appUserId;
    }

    public String getRootFolderId() {
        return rootFolderId;
    }

    public void setRootFolderId(String rootFolderId) {
        this.rootFolderId = rootFolderId;
    }

    public String getDropsFolderId() {
        return dropsFolderId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoxSDKServiceConfigEntity that = (BoxSDKServiceConfigEntity) o;

        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) return false;
        if (clientSecret != null ? !clientSecret.equals(that.clientSecret) : that.clientSecret != null) return false;
        if (publicKeyId != null ? !publicKeyId.equals(that.publicKeyId) : that.publicKeyId != null) return false;
        if (privateKey != null ? !privateKey.equals(that.privateKey) : that.privateKey != null) return false;
        if (privateKeyPassword != null ? !privateKeyPassword.equals(that.privateKeyPassword) : that.privateKeyPassword != null)
            return false;
        if (enterpriseId != null ? !enterpriseId.equals(that.enterpriseId) : that.enterpriseId != null) return false;
        if (appUserId != null ? !appUserId.equals(that.appUserId) : that.appUserId != null) return false;
        if (rootFolderId != null ? !rootFolderId.equals(that.rootFolderId) : that.rootFolderId != null) return false;
        return dropsFolderId != null ? dropsFolderId.equals(that.dropsFolderId) : that.dropsFolderId == null;

    }

    @Override
    public int hashCode() {
        int result = clientId != null ? clientId.hashCode() : 0;
        result = 31 * result + (clientSecret != null ? clientSecret.hashCode() : 0);
        result = 31 * result + (publicKeyId != null ? publicKeyId.hashCode() : 0);
        result = 31 * result + (privateKey != null ? privateKey.hashCode() : 0);
        result = 31 * result + (privateKeyPassword != null ? privateKeyPassword.hashCode() : 0);
        result = 31 * result + (enterpriseId != null ? enterpriseId.hashCode() : 0);
        result = 31 * result + (appUserId != null ? appUserId.hashCode() : 0);
        result = 31 * result + (rootFolderId != null ? rootFolderId.hashCode() : 0);
        result = 31 * result + (dropsFolderId != null ? dropsFolderId.hashCode() : 0);
        return result;
    }

    public void setDropsFolderId(String dropsFolderId) {
        this.dropsFolderId = dropsFolderId;
    }
}
