package com.box.l10n.mojito.service.blobstorage;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Layer on top of {@link BlobStorage} to avoid naming clash between different Mojito services.
 *
 * <p>Use this class instead of using directly {@link BlobStorage}
 */
@Component
public class StructuredBlobStorage {

  @Autowired BlobStorage blobStorage;

  public Optional<String> getString(Prefix prefix, String name) {
    return blobStorage.getString(getFullName(prefix, name));
  }

  public Optional<byte[]> getBytes(Prefix prefix, String name) {
    return blobStorage.getBytes(getFullName(prefix, name));
  }

  public void put(Prefix prefix, String name, String content, Retention retention) {
    blobStorage.put(getFullName(prefix, name), content, retention);
  }

  public void putBytes(Prefix prefix, String name, byte[] content, Retention retention) {
    blobStorage.put(getFullName(prefix, name), content, retention);
  }

  public void delete(Prefix prefix, String name) {
    blobStorage.delete(getFullName(prefix, name));
  }

  public boolean exists(Prefix prefix, String name) {
    return blobStorage.exists(getFullName(prefix, name));
  }

  String getFullName(Prefix prefix, String name) {
    return prefix.toString().toLowerCase() + "/" + name;
  }

  public enum Prefix {
    POLLABLE_TASK,
    IMAGE,
    MULTI_BRANCH_STATE,
    TEXT_UNIT_DTOS_CACHE,
    CLOB_STORAGE_WS,
    AI_TRANSLATE_WS,
    AI_REVIEW_WS
  }
}
