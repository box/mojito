package com.box.l10n.mojito.openai;

/** Minimal HTTP response view used by {@link OpenAIClient} after Apache HttpClient execution. */
record OpenAIHttpStringResponse(int statusCode, String body) {}
