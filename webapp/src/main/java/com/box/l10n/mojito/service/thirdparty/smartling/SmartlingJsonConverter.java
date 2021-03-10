package com.box.l10n.mojito.service.thirdparty.smartling;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.smartling.AssetPathAndTextUnitNameKeys;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class SmartlingJsonConverter {

    ObjectMapper objectMapper;
    AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys;

    @Autowired
    public SmartlingJsonConverter(ObjectMapper objectMapper, AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys) {
        this.objectMapper = objectMapper;
        this.assetPathAndTextUnitNameKeys = assetPathAndTextUnitNameKeys;
    }

    public String textUnitDTOsToJsonString(List<TextUnitDTO> textUnitDTOs, Function<TextUnitDTO, String> toStringFieldValue) {
        ObjectNode document = objectMapper.createObjectNode()
                .<ObjectNode>set("smartling", objectMapper.createObjectNode()
                        .<ObjectNode>set("translate_paths", objectMapper.createObjectNode()
                                .put("path", "*/string")
                                .put("key", "{*}/string")
                                .put("instruction", "*/note")
                        )
                        .put("string_format", "icu")
                );

        textUnitDTOs.stream().forEach(t -> document.set(assetPathAndTextUnitNameKeys.toKey(t.getAssetPath(), t.getName()), objectMapper.createObjectNode()
                .put("tmTextUnitId", t.getTmTextUnitId())
                .put("string", toStringFieldValue.apply(t))
                .put("note", t.getComment())));

        return objectMapper.writeValueAsStringUnchecked(document);
    }

    public ImmutableList<TextUnitDTO> jsonStringToTextUnitDTOs(String localizedFileContent, BiConsumer<TextUnitDTO, String> stringConsumer) {
        ImmutableList<TextUnitDTO> textUnitDTOs = Streams.stream(objectMapper.readTreeUnchecked(localizedFileContent).fields())
                .filter(stringJsonNodeEntry -> !stringJsonNodeEntry.getKey().equals("smartling"))
                .map(stringJsonNodeEntry -> {
                    AssetPathAndTextUnitNameKeys.Key key = assetPathAndTextUnitNameKeys.parse(stringJsonNodeEntry.getKey());
                    TextUnitDTO textUnitDTO = new TextUnitDTO();
                    textUnitDTO.setAssetPath(key.getAssetPath());
                    textUnitDTO.setTmTextUnitId(stringJsonNodeEntry.getValue().get("tmTextUnitId").asLong());
                    textUnitDTO.setName(key.getTextUnitName());
                    textUnitDTO.setComment(stringJsonNodeEntry.getValue().get("note").asText());
                    String string = stringJsonNodeEntry.getValue().get("string").asText();
                    stringConsumer.accept(textUnitDTO, string);
                    return textUnitDTO;
                })
                .collect(ImmutableList.toImmutableList());
        return textUnitDTOs;
    }

}
