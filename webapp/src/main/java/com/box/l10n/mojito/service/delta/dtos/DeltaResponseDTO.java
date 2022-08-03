package com.box.l10n.mojito.service.delta.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO for delta contents.
 *
 * @author garion
 */
public class DeltaResponseDTO {
    @JsonProperty("metadata")
    DeltaMetadataDTO deltaMetadataDTO;

    @JsonProperty("content")
    Map<String, DeltaLocaleDataDTO> translationsPerLocale;

    public DeltaMetadataDTO getDeltaMetadataDTO() {
        return deltaMetadataDTO;
    }

    public void setDeltaMetadataDTO(DeltaMetadataDTO deltaMetadataDTO) {
        this.deltaMetadataDTO = deltaMetadataDTO;
    }

    public Map<String, DeltaLocaleDataDTO> getTranslationsPerLocale() {
        return translationsPerLocale;
    }

    public void setTranslationsPerLocale(Map<String, DeltaLocaleDataDTO> translationsPerLocale) {
        this.translationsPerLocale = translationsPerLocale;
    }
}
