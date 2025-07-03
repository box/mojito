export const ThirdPartySyncAction = Object.freeze({
    PUSH: { name: "PUSH", displayName: "Push", tooltip: "Push strings from Mojito to the third-party service" },
    PULL: { name: "PULL", displayName: "Pull", tooltip: "Pull strings from the third-party service to Mojito" },
    MAP_TEXTUNIT: { name: "MAP_TEXTUNIT", displayName: "Map Text Unit", tooltip: "Automatically link strings between Mojito and the third-party service" },
    PUSH_SCREENSHOT: { name: "PUSH_SCREENSHOT", displayName: "Push Screenshot", tooltip: "Push screenshots from Mojito to the third-party service" },
    PUSH_AI_TRANSLATION: { name: "PUSH_AI_TRANSLATION", displayName: "Push AI Translation", tooltip: "Push AI-generated translations from Mojito to the third-party service" },
});
