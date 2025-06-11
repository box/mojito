package com.box.l10n.mojito.service.tm.textunitdtocache;

import static com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage.Prefix.TEXT_UNIT_DTOS_CACHE;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "l10n.cache.textunit.smile.enabled", havingValue = "true")
public class TextUnitDTOsSmileCacheBlobStorage extends TextUnitDTOsCacheBlobStorage {

  @Autowired
  @Qualifier("smile_format_object_mapper")
  ObjectMapper objectMapper;

  String getName(Long assetId, Long localeId) {
    return "asset/" + assetId + "/locale/" + localeId + ".smile";
  }

  Optional<ImmutableList<TextUnitDTO>> getTextUnitsFromCache(Long assetId, Long localeId) {
    Optional<byte[]> bytes =
        redisStructuredBlobStorageProxy.getBytes(TEXT_UNIT_DTOS_CACHE, getName(assetId, localeId));
    return bytes.map(this::convertToListOrEmptyList);
  }

  void writeTextUnitDTOsToCache(
      Long assetId,
      Long localeId,
      TextUnitDTOsCacheBlobStorageJson textUnitDTOsCacheBlobStorageJson) {
    byte[] bytes = objectMapper.writeValueAsBytes(textUnitDTOsCacheBlobStorageJson);
    redisStructuredBlobStorageProxy.putBytes(
        TEXT_UNIT_DTOS_CACHE, getName(assetId, localeId), bytes, Retention.PERMANENT);
  }

  ImmutableList<TextUnitDTO> convertToListOrEmptyList(byte[] s) {
    try {
      return ImmutableList.copyOf(
          objectMapper.readValue(s, TextUnitDTOsCacheBlobStorageJson.class).getTextUnitDTOs());
    } catch (Exception e) {
      logger.error(
          "Can't convert the content into TextUnitDTOsCacheBlobStorageJson, return an empty list instead",
          e);
      return ImmutableList.of();
    }
  }
}
