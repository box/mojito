package com.box.l10n.mojito.service.ai.openai;

public enum OpenAIPromptContextMessageType {
  SYSTEM("system"),
  USER("user"),
  ASSISTANT("assistant");

  private final String type;

  OpenAIPromptContextMessageType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
