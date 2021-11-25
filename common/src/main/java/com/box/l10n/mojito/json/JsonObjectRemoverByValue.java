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
     * Removes the objects that contain the specified value from the input JSON and returns the indented result.
     * <p>
     * - if the object: o1 that contains the specified value is in an array: a1 then o1 is removed from a1
     * - if the object: o1 that contains the specified value is in another object: o2 (for attribute name: an1) then
     * the field: "an1 : o1" is removed from o2
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
        removeFieldsWithValueFromObject(objectNode, valueToRemove);
        // recurse on others
        objectNode.iterator().forEachRemaining(jsonNode -> remove(jsonNode, valueToRemove));
    }

    static void removeInArray(ArrayNode arrayNode, String valueToRemove) {
        removeObjectsWithValueFromArray(arrayNode, valueToRemove);
        // recurse on others
        arrayNode.elements().forEachRemaining(jsonNode -> remove(jsonNode, valueToRemove));
    }

    static void removeFieldsWithValueFromObject(ObjectNode objectNode, String valueToRemove) {
        List<String> toRemove = Streams.stream(objectNode.fields())
                .filter(f -> isTextutalFieldWithValue(valueToRemove, f.getValue())
                        || isObjectFieldWithNestedValue(valueToRemove, f.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        toRemove.forEach(objectNode::remove);
    }

    static boolean isObjectFieldWithNestedValue(String valueToRemove, JsonNode jsonNode) {
        return Streams.stream(jsonNode.fields())
                .filter(f -> f.getValue().asText().equals(valueToRemove))
                .findFirst()
                .map(Map.Entry::getKey)
                .isPresent();
    }

    static boolean isTextutalFieldWithValue(String valueToRemove, JsonNode jsonNode) {
        return jsonNode.isTextual() && jsonNode.asText().equals(valueToRemove);
    }

    static void removeObjectsWithValueFromArray(ArrayNode arrayNode, String valueToRemove) {
        List<Integer> toRemove = IntStream.range(0, arrayNode.size())
                .filter(idx -> isObjectFieldWithNestedValue(valueToRemove, arrayNode.get(idx)))
                .boxed().collect(Collectors.toList());
        toRemove.forEach(arrayNode::remove);
    }
}
