import alt from "../../alt.js";
import TextUnitDataSource from "../../actions/workbench/TextUnitDataSource.js";
import TranslationHistoryActions from "../../actions/workbench/TranslationHistoryActions.js";

class TranslationHistoryStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(TranslationHistoryActions);
        this.registerAsync(TextUnitDataSource);
    }

    setDefaultState() {
        this.show = false;
        this.textUnit = null;
        this.translationHistory = null;
        this.loading = false;
        this.openTmTextUnitVariantId = null;
    }

    close() {
        this.show = false;
    }

    openWithTextUnit(textUnit) {
        this.show = true;
        this.textUnit = textUnit;
        this.translationHistory = null;
        this.loading = true;
        this.getInstance().getTranslationHistory(textUnit);
    }

    onGetTranslationHistorySuccess(translationHistory) {
        this.translationHistory = translationHistory;
        this.loading = false;
    }

    onGetTranslationHistoryError() {
        this.loading = false;
    }

    changeOpenTmTextUnitVariant(tmTextUnitVariantId) {
        this.openTmTextUnitVariantId = tmTextUnitVariantId;
    }
}

export default alt.createStore(TranslationHistoryStore, 'TranslationHistoryStore');
