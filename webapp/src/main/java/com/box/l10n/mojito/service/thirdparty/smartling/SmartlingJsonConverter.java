package com.box.l10n.mojito.service.thirdparty.smartling;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.smartling.SmartlingJsonKeys;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SmartlingJsonConverter {

  ObjectMapper objectMapper;
  SmartlingJsonKeys smartlingJsonKeys;

  @Autowired
  public SmartlingJsonConverter(ObjectMapper objectMapper, SmartlingJsonKeys smartlingJsonKeys) {
    this.objectMapper = objectMapper;
    this.smartlingJsonKeys = smartlingJsonKeys;
  }

  public String textUnitDTOsToJsonString(
      List<TextUnitDTO> textUnitDTOs, Function<TextUnitDTO, String> toStringFieldValue) {
    ObjectNode document =
        objectMapper
            .createObjectNode()
            .<ObjectNode>set(
                "smartling",
                objectMapper
                    .createObjectNode()
                    .<ObjectNode>set(
                        "translate_paths",
                        objectMapper
                            .createObjectNode()
                            .put("path", "*/string")
                            .put("instruction", "*/note")
                            .put("key", "*/key"))
                    .put("string_format", "icu")
                    .put("variants_enabled", "true"));

    ArrayNode strings = objectMapper.createArrayNode();
    document.put("strings", strings);
    textUnitDTOs.stream()
        .forEach(
            t ->
                strings.add(
                    objectMapper
                        .createObjectNode()
                        .put(
                            "key",
                            smartlingJsonKeys.toKey(
                                t.getTmTextUnitId(), t.getName(), t.getAssetPath()))
                        .put("tmTextUnitId", t.getTmTextUnitId())
                        .put("assetPath", t.getAssetPath())
                        .put("name", t.getName())
                        .put("string", toStringFieldValue.apply(t))
                        .put("note", t.getComment())));

    return objectMapper.writeValueAsStringUnchecked(document);
  }

  public ImmutableList<TextUnitDTO> jsonStringToTextUnitDTOs(
      String localizedFileContent, BiConsumer<TextUnitDTO, String> stringConsumer) {
    ImmutableList<TextUnitDTO> textUnitDTOs =
        Streams.stream(
                objectMapper.readTreeUnchecked(localizedFileContent).get("strings").iterator())
            .map(
                node -> {
                  TextUnitDTO textUnitDTO = new TextUnitDTO();
                  textUnitDTO.setAssetPath(node.get("assetPath").asText());
                  textUnitDTO.setTmTextUnitId(node.get("tmTextUnitId").asLong());
                  textUnitDTO.setName(node.get("name").asText());
                  textUnitDTO.setComment(node.get("note").asText());
                  String string = node.get("string").asText();
                  stringConsumer.accept(textUnitDTO, string);
                  return textUnitDTO;
                })
            .collect(ImmutableList.toImmutableList());
    return textUnitDTOs;
  }
}
