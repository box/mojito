import alt from "../../alt.js";

class TranslationHistoryActions {

    constructor() {
        this.generateActions(
            "openWithTextUnit",
            "getTranslationHistorySuccess",
            "getTranslationHistoryError",
            "close",
            "changeOpenTmTextUnitVariant"
        );
    }
}

export default alt.createActions(TranslationHistoryActions);
