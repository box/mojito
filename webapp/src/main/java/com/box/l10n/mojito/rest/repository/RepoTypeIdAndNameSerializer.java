package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.entity.RepoType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

/** Serializes a repo type nested in a repository without exposing its shared configuration. */
public class RepoTypeIdAndNameSerializer extends JsonSerializer<RepoType> {

  @Override
  public void serialize(RepoType repoType, JsonGenerator generator, SerializerProvider serializers)
      throws IOException {
    generator.writeStartObject();
    generator.writeNumberField("id", repoType.getId());
    generator.writeStringField("name", repoType.getName());
    generator.writeEndObject();
  }
}
