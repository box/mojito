package com.box.l10n.mojito.service.blobstorage;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Low level API for blob storage supporting basic retention policy.
 *
 * put() methods should override content if an empty entry already exists for the given name.
 */
public interface BlobStorage {

    Optional<byte[]> getBytes(String name);

    void put(String name, byte[] content, Retention retention);

    void delete(String name);

    boolean exists(String name);

    default Optional<String> getString(String name) {
        return getBytes(name).map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    default void put(String name, String content) {
        put(name, content, Retention.PERMANENT);
    };

    default void put(String name, String content, Retention retention) {
        put(name, content.getBytes(StandardCharsets.UTF_8), retention);
    }

    default void put(String name, byte[] content) {
        put(name, content, Retention.PERMANENT);
    }
}
