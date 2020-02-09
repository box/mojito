package com.box.l10n.mojito.json;

import com.box.l10n.mojito.io.Files;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * Extends {@link com.fasterxml.jackson.databind.ObjectMapper} to provide
 * convenient methods.
 *
 * @author jaurambault
 */
public class ObjectMapper extends com.fasterxml.jackson.databind.ObjectMapper {

    public ObjectMapper() {
        JodaModule jodaModule = new JodaModule();
        registerModule(jodaModule);
    }

    /**
     * Same as {@link #writeValueAsString(java.lang.Object) but throws
     * {@link RuntimeException} instead of {@link JsonProcessingException}
     *
     * @param value
     * @return
     */
    public String writeValueAsStringUnchecked(Object value) {
        try {
            return super.writeValueAsString(value);
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException(jpe);
        }
    }

    public <T> T readValueUnchecked(File src, Class<T> valueType) {
        try {
            return super.readValue(src, valueType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public <T> T readValueUnchecked(String content, Class<T> valueType) {
        try {
            return super.readValue(content, valueType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeValueUnchecked(File file, Object content) {
        try {
            super.writeValue(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public JsonNode readTreeUnchecked(String content) {
        try {
            return super.readTree(content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void createDirectoriesAndWrite(Path path, Object content) {
        Files.createDirectories(path.getParent());
        writeValueUnchecked(path.toFile(), content);
    }

}
