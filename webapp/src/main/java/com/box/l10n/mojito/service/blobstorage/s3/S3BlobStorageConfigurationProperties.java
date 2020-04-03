package com.box.l10n.mojito.service.blobstorage.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.blob-storage.s3")
public class S3BlobStorageConfigurationProperties {

    String bucket = "mojito";
    String prefix = "mojito";

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
