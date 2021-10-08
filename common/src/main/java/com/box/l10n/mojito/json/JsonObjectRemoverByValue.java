package com.box.l10n.mojito.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JsonObjectRemoverByValue {

    /**
     * Removes the objects that contain the specified value (attribute name is doesn't matter) from the input JSON and
     * returns the indented result.
     * - if the object: o1 contains the specified value is in an array: a1 the o1 is removed from the a1
     * - if the object: o1 contains the specified value (for attribute name: n1) is in another object: o2,
     * the filed: "n1 : o1" is removed from o2
     *
     * @param jsonContent
     * @param valueToRemove
     * @return identedContent without objects that contain the specified value.
     */
    public static String remove(String jsonContent, String valueToRemove) {
        ObjectMapper objectMapper = ObjectMapper.withIndentedOutput();
        JsonNode jsonNode = objectMapper.readTreeUnchecked(jsonContent);
        remove(jsonNode, valueToRemove);
        return objectMapper.writeValueAsStringUnchecked(jsonNode);
    }

    static void remove(JsonNode jsonNode, String valueToRemove) {
        if (jsonNode.isObject()) {
            removeInObject((ObjectNode) jsonNode, valueToRemove);
        } else if (jsonNode.isArray()) {
            removeInArray((ArrayNode) jsonNode, valueToRemove);
        }
    }

    static void removeInObject(ObjectNode objectNode, String valueToRemove) {
        removeObjectsWithValueFromObject(objectNode, valueToRemove);
        // recurse on others
        objectNode.iterator().forEachRemaining(jsonNode -> remove(jsonNode, valueToRemove));
    }

    static void removeInArray(ArrayNode arrayNode, String valueToRemove) {
        removeObjectsWithValueFromArray(arrayNode, valueToRemove);
        // recurse on others
        arrayNode.elements().forEachRemaining(jsonNode -> remove(jsonNode, valueToRemove));
    }

    static void removeObjectsWithValueFromObject(ObjectNode objectNode, String valueToRemove) {
        List<String> toRemove = Streams.stream(objectNode.fieldNames())
                .filter(n -> Streams.stream(objectNode.get(n).fields())
                        .filter(sn -> sn.getValue().asText().equals(valueToRemove))
                        .findFirst()
                        .map(Map.Entry::getKey)
                        .isPresent())
                .collect(Collectors.toList());
        toRemove.forEach(objectNode::remove);
    }

    static void removeObjectsWithValueFromArray(ArrayNode arrayNode, String valueToRemove) {
        List<Integer> toRemove = IntStream.range(0, arrayNode.size())
                .filter(idx -> Streams.stream(arrayNode.get(idx).fields())
                        .filter(sn -> sn.getValue().asText().equals(valueToRemove))
                        .findFirst()
                        .map(Map.Entry::getKey)
                        .isPresent())
                .boxed().collect(Collectors.toList());
        toRemove.forEach(arrayNode::remove);
    }
}
