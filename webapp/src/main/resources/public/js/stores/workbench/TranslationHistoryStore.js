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
    }

    close() {
        this.show = false;
    }

    openWithTextUnit(textUnit) {
        console.log("TranslationHistoryStore::openWithTextUnit");

        this.show = true;
        this.textUnit = textUnit;
        this.translationHistory = null;
        this.loading = true;
        this.getInstance().getTranslationHistory(textUnit);
    }

    onGetTranslationHistorySuccess(translationHistory) {
        console.log("TranslationHistoryStore::onGetTranslationHistorySuccess");
        this.translationHistory = translationHistory;
        this.loading = false;
    }

    onGetTranslationHistoryError(errorResponse) {
        console.log("TranslationHistoryStore::onGetTranslationHistoryError");
        this.loading = false;
    }
}

export default alt.createStore(TranslationHistoryStore, 'TranslationHistoryStore');
