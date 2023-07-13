import alt from "../../alt";
import Error from "../../utils/Error";
import TextUnit from "../../sdk/TextUnit";
import TextUnitDataSource from "../../actions/workbench/TextUnitDataSource";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchParamsStore from "./SearchParamsStore";
import {StatusCommonTypes} from "../../components/screenshots/StatusCommon";
import TranslationHistoryActions from "../../actions/workbench/TranslationHistoryActions";

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

    onGetTranslationHistoryError(errorResponse) {
        this.loading = false;
    }

    changeOpenTmTextUnitVariant(tmTextUnitVariantId) {
        this.openTmTextUnitVariantId = tmTextUnitVariantId;
    }
}

export default alt.createStore(TranslationHistoryStore, 'TranslationHistoryStore');
