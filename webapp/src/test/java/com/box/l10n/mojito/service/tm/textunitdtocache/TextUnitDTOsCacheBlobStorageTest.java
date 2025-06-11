package com.box.l10n.mojito.service.tm.textunitdtocache;

import static com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage.Prefix.TEXT_UNIT_DTOS_CACHE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TextUnitDTOsCacheBlobStorageTest extends ServiceTestBase {

  @Autowired TextUnitDTOsCacheBlobStorage textUnitDTOsCacheBlobStorage;

  @Test
  public void readInvalidData() {
    long assetId = 123243L;
    long localeId = 234L;
    textUnitDTOsCacheBlobStorage.redisStructuredBlobStorageProxy.put(
        TEXT_UNIT_DTOS_CACHE,
        textUnitDTOsCacheBlobStorage.getName(assetId, localeId),
        "bad content",
        Retention.PERMANENT);
    List<TextUnitDTO> textUnitDTOS =
        textUnitDTOsCacheBlobStorage.getTextUnitDTOs(assetId, localeId).get();
    assertEquals(
        "Should be empty (if not make sure the test is not run with a store that as data for that entry",
        textUnitDTOS,
        Collections.emptyList());
  }

  @Test
  public void readNoData() {
    long assetId = 123243L;
    long localeId = 234L;
    textUnitDTOsCacheBlobStorage.redisStructuredBlobStorageProxy.delete(
        TEXT_UNIT_DTOS_CACHE, textUnitDTOsCacheBlobStorage.getName(assetId, localeId));
    Optional<ImmutableList<TextUnitDTO>> textUnitDTOS =
        textUnitDTOsCacheBlobStorage.getTextUnitDTOs(assetId, localeId);
    assertFalse(textUnitDTOS.isPresent());
  }

  @Test
  public void writeAndRead() {
    TextUnitDTO textUnitDTO = new TextUnitDTO();
    textUnitDTO.setName(UUID.randomUUID().toString());
    ImmutableList<TextUnitDTO> textUnitDTOSToWrite = ImmutableList.of(textUnitDTO);
    textUnitDTOsCacheBlobStorage.putTextUnitDTOs(12345L, 12345L, textUnitDTOSToWrite);
    List<TextUnitDTO> readTextUnitDTOS =
        textUnitDTOsCacheBlobStorage.getTextUnitDTOs(12345L, 12345L).get();

    Assertions.assertThat(readTextUnitDTOS)
        .usingFieldByFieldElementComparator()
        .containsExactlyElementsOf(textUnitDTOSToWrite);
  }

  @Test
  public void getName() {
    String blobName = textUnitDTOsCacheBlobStorage.getName(1234L, 56L);
    assertEquals("asset/1234/locale/56", blobName);
  }

  @Test
  public void convertToListOrEmptyList() {
    ImmutableList<TextUnitDTO> textUnitDTOS =
        textUnitDTOsCacheBlobStorage.convertToListOrEmptyList("some bad content for testing");
    assertEquals(ImmutableList.of(), textUnitDTOS);
  }
}
