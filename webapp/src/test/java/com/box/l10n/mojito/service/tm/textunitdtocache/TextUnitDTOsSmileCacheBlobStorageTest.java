package com.box.l10n.mojito.service.tm.textunitdtocache;

import static com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage.Prefix.TEXT_UNIT_DTOS_CACHE;
import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TextUnitDTOsSmileCacheBlobStorageTest {

  TextUnitDTOsSmileCacheBlobStorage textUnitDTOsCacheBlobStorage =
      new TextUnitDTOsSmileCacheBlobStorage();

  ObjectMapper objectMapper = ObjectMapper.withSmileEnabled();

  @Before
  public void setUp() {
    textUnitDTOsCacheBlobStorage.objectMapper = objectMapper;
  }

  @Test
  public void testSuffixAppendedToName() {
    String blobName = textUnitDTOsCacheBlobStorage.getName(1234L, 56L);
    assertEquals("asset/1234/locale/56.smile", blobName);
  }

  @Test
  public void testBytesWrittenToCache() {
    StructuredBlobStorage structuredBlobStorageMock = Mockito.mock(StructuredBlobStorage.class);
    TextUnitDTOsCacheBlobStorageJson textUnitDTOsCacheBlobStorageJson =
        new TextUnitDTOsCacheBlobStorageJson();
    textUnitDTOsCacheBlobStorage.structuredBlobStorage = structuredBlobStorageMock;
    byte[] expectedBytes = objectMapper.writeValueAsBytes(textUnitDTOsCacheBlobStorageJson);
    textUnitDTOsCacheBlobStorage.writeTextUnitDTOsToCache(
        1234L, 56L, textUnitDTOsCacheBlobStorageJson);
    Mockito.verify(structuredBlobStorageMock)
        .putBytes(
            TEXT_UNIT_DTOS_CACHE, "asset/1234/locale/56.smile", expectedBytes, Retention.PERMANENT);
  }

  @Test
  public void testGetTextUnitDTOS() throws IOException {
    StructuredBlobStorage structuredBlobStorageMock = Mockito.mock(StructuredBlobStorage.class);
    TextUnitDTOsCacheBlobStorageJson textUnitDTOsCacheBlobStorageJson =
        new TextUnitDTOsCacheBlobStorageJson();
    TextUnitDTO textUnitDTO = new TextUnitDTO();
    textUnitDTO.setComment("testComment");
    textUnitDTO.setTmTextUnitId(1L);
    textUnitDTO.setName("testName");
    textUnitDTO.setSource("testSource");
    textUnitDTOsCacheBlobStorageJson.setTextUnitDTOs(ImmutableList.of(textUnitDTO));
    byte[] expectedBytes = objectMapper.writeValueAsBytes(textUnitDTOsCacheBlobStorageJson);
    Mockito.when(
            structuredBlobStorageMock.getBytes(TEXT_UNIT_DTOS_CACHE, "asset/1234/locale/56.smile"))
        .thenReturn(Optional.of(expectedBytes));
    textUnitDTOsCacheBlobStorage.structuredBlobStorage = structuredBlobStorageMock;
    Optional<ImmutableList<TextUnitDTO>> textUnitDTOS =
        textUnitDTOsCacheBlobStorage.getTextUnitsFromCache(1234L, 56L);
    Mockito.verify(structuredBlobStorageMock)
        .getBytes(TEXT_UNIT_DTOS_CACHE, "asset/1234/locale/56.smile");
    assertEquals(1, textUnitDTOS.get().size());
    assertEquals("testComment", textUnitDTOS.get().get(0).getComment());
    assertEquals(1L, textUnitDTOS.get().get(0).getTmTextUnitId().longValue());
    assertEquals("testName", textUnitDTOS.get().get(0).getName());
    assertEquals("testSource", textUnitDTOS.get().get(0).getSource());
  }
}
