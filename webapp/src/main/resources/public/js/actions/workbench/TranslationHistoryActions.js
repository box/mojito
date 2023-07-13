import alt from "../../alt";

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
