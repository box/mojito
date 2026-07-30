package com.box.l10n.mojito.openai;

import java.util.List;

/** Line-oriented HTTP response used for SSE-style OpenAI streams. */
record OpenAIHttpLinesResponse(int statusCode, List<String> lines) {}
