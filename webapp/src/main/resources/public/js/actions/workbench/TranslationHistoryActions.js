import alt from "../../alt";

class TranslationHistoryActions {

    constructor() {
        this.generateActions(
            "openWithTextUnit",
            "getTranslationHistorySuccess",
            "getTranslationHistoryError",
            "close"
        );
    }
}

export default alt.createActions(TranslationHistoryActions);
