package com.box.l10n.mojito.service.blobstorage;

/** Retention policy for blobs. */
public enum Retention {
  /** For blobs that must be kept forever */
  PERMANENT,
  /** For blobs that should be kept at least one day */
  MIN_1_DAY
}
