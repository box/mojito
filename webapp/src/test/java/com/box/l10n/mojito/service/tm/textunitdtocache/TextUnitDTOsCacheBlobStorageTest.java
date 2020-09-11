package com.box.l10n.mojito.service.tm.textunitdtocache;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class TextUnitDTOsCacheBlobStorageTest extends ServiceTestBase {

    @Autowired
    TextUnitDTOsCacheBlobStorage textUnitDTOsCacheBlobStorage;

    @Test
    public void readNoData() {
        List<TextUnitDTO> textUnitDTOS = textUnitDTOsCacheBlobStorage.getTextUnitDTOs(123243L, 234L);
        assertEquals("Should be empty (if not make sure the test is not run with a store that as data for that entry", textUnitDTOS, Collections.emptyList());
    }

    @Test
    public void writeAndRead() {
        TextUnitDTO textUnitDTO = new TextUnitDTO();
        textUnitDTO.setName(UUID.randomUUID().toString());
        ImmutableList<TextUnitDTO> textUnitDTOSToWrite = ImmutableList.of(textUnitDTO);
        textUnitDTOsCacheBlobStorage.putTextUnitDTOs(12345L, 12345L, textUnitDTOSToWrite);
        List<TextUnitDTO> readTextUnitDTOS = textUnitDTOsCacheBlobStorage.getTextUnitDTOs(12345L, 12345L);

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
        ImmutableList<TextUnitDTO> textUnitDTOS = textUnitDTOsCacheBlobStorage.convertToListOrEmptyList("some bad content for testing");
        assertEquals(ImmutableList.of(), textUnitDTOS);
    }

}