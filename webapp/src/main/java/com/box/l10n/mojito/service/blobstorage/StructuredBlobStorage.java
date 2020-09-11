package com.box.l10n.mojito.service.blobstorage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Layer on top of {@link BlobStorage} to avoid naming clash between different Mojito services.
 *
 * Use this class instead of using directly {@link BlobStorage}
 */
@Component
public class StructuredBlobStorage {

    @Autowired
    BlobStorage blobStorage;

    public Optional<String> getString(Prefix prefix, String name) {
        return blobStorage.getString(getFullName(prefix, name));
    }

    public void put(Prefix prefix, String name, String content, Retention retention) {
        blobStorage.put(getFullName(prefix, name), content, retention);
    }

    String getFullName(Prefix prefix, String name) {
        return prefix.toString().toLowerCase() + "/" + name;
    }

    public enum Prefix {
        POLLABLE_TASK,
        IMAGE,
        MERGE_STATE,
        TEXT_UNIT_DTOS_CACHE
    }
}
