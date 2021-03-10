package com.box.l10n.mojito.service.thirdparty.smartling;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.smartling.AssetPathAndTextUnitNameKeys;
import com.box.l10n.mojito.smartling.SmartlingJsonKeys;
import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.stream.IntStream;

public class SmartlingJsonConverterTest {
    SmartlingJsonConverter smartlingJsonConverter = new SmartlingJsonConverter(ObjectMapper.withIndentedOutput(), new SmartlingJsonKeys());

    @Test
    public void textUnitDTOsToJsonString() {
        String s = smartlingJsonConverter.textUnitDTOsToJsonString(
                IntStream.range(0, 2).mapToObj(idx -> {
                    TextUnitDTO textUnitDTO = new TextUnitDTO();
                    textUnitDTO.setTmTextUnitId((long) idx);
                    textUnitDTO.setAssetPath("assetPath");
                    textUnitDTO.setName("name-" + idx);
                    textUnitDTO.setSource("source-" + idx);
                    textUnitDTO.setComment("comment-" + idx);
                    return textUnitDTO;
                }).collect(ImmutableList.toImmutableList()), TextUnitDTO::getSource);

        Assertions.assertThat(s).isEqualTo("{\n" +
                "  \"smartling\" : {\n" +
                "    \"translate_paths\" : {\n" +
                "      \"path\" : \"*/string\",\n" +
                "      \"instruction\" : \"*/note\",\n" +
                "      \"key\" : \"*/key\"\n" +
                "    },\n" +
                "    \"string_format\" : \"icu\",\n" +
                "    \"variants_enabled\" : \"true\"\n" +
                "  },\n" +
                "  \"strings\" : [ {\n" +
                "    \"key\" : \"0#@#assetPath#@#name-0\",\n" +
                "    \"tmTextUnitId\" : 0,\n" +
                "    \"assetPath\" : \"assetPath\",\n" +
                "    \"name\" : \"name-0\",\n" +
                "    \"string\" : \"source-0\",\n" +
                "    \"note\" : \"comment-0\"\n" +
                "  }, {\n" +
                "    \"key\" : \"1#@#assetPath#@#name-1\",\n" +
                "    \"tmTextUnitId\" : 1,\n" +
                "    \"assetPath\" : \"assetPath\",\n" +
                "    \"name\" : \"name-1\",\n" +
                "    \"string\" : \"source-1\",\n" +
                "    \"note\" : \"comment-1\"\n" +
                "  } ]\n" +
                "}");
    }

    @Test
    public void testNameDuplicates() {
        String s = smartlingJsonConverter.textUnitDTOsToJsonString(
                IntStream.range(0, 2).mapToObj(idx -> {
                    TextUnitDTO textUnitDTO = new TextUnitDTO();
                    textUnitDTO.setTmTextUnitId((long) idx);
                    textUnitDTO.setAssetPath("assetPath");
                    textUnitDTO.setName("name");
                    textUnitDTO.setSource("source-" + idx);
                    textUnitDTO.setComment("comment-" + idx);
                    return textUnitDTO;
                }).collect(ImmutableList.toImmutableList()), TextUnitDTO::getSource);

        Assertions.assertThat(s).isEqualTo("{\n" +
                "  \"smartling\" : {\n" +
                "    \"translate_paths\" : {\n" +
                "      \"path\" : \"*/string\",\n" +
                "      \"instruction\" : \"*/note\",\n" +
                "      \"key\" : \"*/key\"\n" +
                "    },\n" +
                "    \"string_format\" : \"icu\",\n" +
                "    \"variants_enabled\" : \"true\"\n" +
                "  },\n" +
                "  \"strings\" : [ {\n" +
                "    \"key\" : \"0#@#assetPath#@#name\",\n" +
                "    \"tmTextUnitId\" : 0,\n" +
                "    \"assetPath\" : \"assetPath\",\n" +
                "    \"name\" : \"name\",\n" +
                "    \"string\" : \"source-0\",\n" +
                "    \"note\" : \"comment-0\"\n" +
                "  }, {\n" +
                "    \"key\" : \"1#@#assetPath#@#name\",\n" +
                "    \"tmTextUnitId\" : 1,\n" +
                "    \"assetPath\" : \"assetPath\",\n" +
                "    \"name\" : \"name\",\n" +
                "    \"string\" : \"source-1\",\n" +
                "    \"note\" : \"comment-1\"\n" +
                "  } ]\n" +
                "}");
    }

    @Test
    public void jsonStringToTextUnitDTOs() {
        String jsonString = "{\n" +
                "  \"smartling\" : {\n" +
                "    \"translate_paths\" : {\n" +
                "      \"path\" : \"*/string\",\n" +
                "      \"instruction\" : \"*/note\",\n" +
                "      \"key\" : \"*/key\"\n" +
                "    },\n" +
                "    \"string_format\" : \"icu\",\n" +
                "    \"variants_enabled\" : \"true\"\n" +
                "  },\n" +
                "  \"strings\" : [ {\n" +
                "    \"key\" : \"0#@#assetPath#@#name-0\",\n" +
                "    \"tmTextUnitId\" : 0,\n" +
                "    \"assetPath\" : \"assetPath\",\n" +
                "    \"name\" : \"name-0\",\n" +
                "    \"string\" : \"target-0\",\n" +
                "    \"note\" : \"comment-0\"\n" +
                "  }, {\n" +
                "    \"key\" : \"1#@#assetPath#@#name-1\",\n" +
                "    \"tmTextUnitId\" : 1,\n" +
                "    \"assetPath\" : \"assetPath\",\n" +
                "    \"name\" : \"name-1\",\n" +
                "    \"string\" : \"target-1\",\n" +
                "    \"note\" : \"comment-1\"\n" +
                "  } ]\n" +
                "}";

        ImmutableList<TextUnitDTO> textUnitDTOS = smartlingJsonConverter.jsonStringToTextUnitDTOs(jsonString, TextUnitDTO::setTarget);
        Assertions.assertThat(textUnitDTOS)
                .usingRecursiveComparison()
                .isEqualTo(IntStream.range(0, 2).mapToObj(idx -> {
                            TextUnitDTO textUnitDTO = new TextUnitDTO();
                            textUnitDTO.setAssetPath("assetPath");
                            textUnitDTO.setTmTextUnitId((long) idx);
                            textUnitDTO.setName("name-" + idx);
                            textUnitDTO.setTarget("target-" + idx);
                            textUnitDTO.setComment("comment-" + idx);
                            return textUnitDTO;
                        }).collect(ImmutableList.toImmutableList())
                );
    }
}