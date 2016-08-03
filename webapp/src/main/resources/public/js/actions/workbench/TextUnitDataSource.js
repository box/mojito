import Error from "../../utils/Error";
import TextUnitError from "../../utils/TextUnitError";
import TextUnitClient from "../../sdk/TextUnitClient";
import WorkbenchActions from "./WorkbenchActions";

const TextUnitDataSource = {
    performSaveTextUnit: {
        remote(searchResultsStoreState, textUnit) {
            return TextUnitClient.saveTextUnit(textUnit)
                .catch(error => {
                    throw new TextUnitError(Error.IDS.TEXTUNIT_SAVE_FAILED, textUnit);
                });
        },
        success: WorkbenchActions.saveTextUnitSuccess,
        error: WorkbenchActions.saveTextUnitError
    },
    performCheckAndSaveTextUnit: {
        remote(searchResultsStoreState, textUnit) {
            return TextUnitClient.checkTextUnitIntegrity(textUnit)
                .then(checkResult => {
                    if (checkResult && !checkResult.checkResult) {
                        throw new TextUnitError(Error.IDS.TEXTUNIT_CHECK_FAILED, textUnit);
                    }
                }).then(() => {
                    return TextUnitClient.saveTextUnit(textUnit)
                        .catch(error => {
                            throw new TextUnitError(Error.IDS.TEXTUNIT_SAVE_FAILED, textUnit);
                        });
                });
        },
        success: WorkbenchActions.checkAndSaveTextUnitSuccess,
        error: WorkbenchActions.checkAndSaveTextUnitError
    },
    deleteTextUnit: {
        remote(searchResultsStoreState, textUnit) {
            return TextUnitClient.deleteCurrentTranslation(textUnit)
                .catch(error => {
                    throw new TextUnitError(Error.IDS.TEXTUNIT_DELETE_FAILED, textUnit);
                });
        },
        success: WorkbenchActions.deleteTextUnitsSuccess,
        error: WorkbenchActions.deleteTextUnitsError
    }
};

export default TextUnitDataSource;
