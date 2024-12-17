import alt from "../../alt";
import Error from "../../utils/Error";
import TextUnit from "../../sdk/TextUnit";
import TextUnitDataSource from "../../actions/workbench/TextUnitDataSource";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";

class TextUnitStore {

    constructor() {
        /** @type {TextUnitError[]} */
        this.errorsKeyedByTextUnitKey = [];

        this.bindActions(WorkbenchActions);

        this.registerAsync(TextUnitDataSource);
    }

    /**
     * @param {TextUnit} textUnit The SDK TextUnit object returned by the server after saving it.
     */
    onSaveTextUnit(textUnit) {
        this.getInstance().performSaveTextUnit(textUnit);
    }

    /**
     * Handle onSuccess event of onSaveTextUnit
     * @param {TextUnit} textUnit The textUnit passed back by the SDK
     */
    onSaveTextUnitSuccess(textUnit) {
        console.log("onSaveTextUnitSuccess");
        this.resetErrorState(textUnit);
    }

    /**
     * Creates an Error object and save the errorResponse and Error objects for the components to use.
     * @param {TextUnitError} errorResponse The error object returned by the promise.
     */
    onSaveTextUnitError(errorResponse) {
        console.log("onSaveTextUnitError");
        this.setErrorState(errorResponse);
    }

    /**
     * @param {TextUnit} textUnit The SDK TextUnit object returned by the server after saving it.
     */
    onCheckAndSaveTextUnit(textUnit) {
        this.getInstance().performCheckAndSaveTextUnit(textUnit);
    }

    /**
     * Creates an Error object and save the errorResponse and Error objects for the components to use.
     * @param {TextUnitError} errorResponse The error object returned by the promise.
     */
    onCheckAndSaveTextUnitError(errorResponse) {
        console.log("onCheckAndSaveTextUnitError");
        this.setErrorState(errorResponse);
    }

    /**
     * Handle onSuccess event of checkAndSaveTextUnit
     * @param {TextUnit} textUnit The textUnit passed back by the SDK
     */
    onCheckAndSaveTextUnitSuccess(textUnit) {
        console.log('onCheckAndSaveTextUnitSuccess');
        this.resetErrorState(textUnit);
    }

    /**
     * The target string of the selected textunits will be deleted if the request is processed successfully.
     * @param {TextUnit[]} textUnits
     */
    onDeleteTextUnits(textUnits) {
        console.log("TextUnitStore::onDeleteTextUnits");
        textUnits.forEach(textUnit => {
            // NOTE: specifically check for null b'c target can be an empty string "" which should still be deleted
            if (textUnit.getTarget() !== null) {
                this.getInstance().deleteTextUnit(textUnit);
            }
        });
    }

    /**
     * @param {TextUnit} textUnit response The response sent by the promise when the delete request succeeds
     */
    onDeleteTextUnitsSuccess(textUnit) {
        console.log("TextUnitStore::onDeleteTextUnitsSuccess");
        this.resetErrorState(textUnit);
    }

    /**
     * @param {TextUnitError} errorResponse
     */
    onDeleteTextUnitsError(errorResponse) {
        console.log("TextUnitStore::onDeleteTextUnitsError");
        this.setErrorState(errorResponse);
    }

    onSaveVirtualAssetTextUnit(textUnit) {
        this.getInstance().saveVirtualAssetTextUnit(textUnit);
    }

    /**
     * @param {TextUnitError} errorResponse
     */
    onSaveVirtualAssetTextUnitError(errorResponse) {
        this.setErrorState(errorResponse);
    }

    /**
     * @param {TextUnit} textUnit
     */
    resetErrorState(textUnit) {
        if (!textUnit) {
            this.errorsKeyedByTextUnitKey = [];
            return;
        }
        delete this.errorsKeyedByTextUnitKey[textUnit.getTextUnitKey()];
    }

    /**
     * @param {string} errorId An error ID from the list of Error.IDS defined in class Error.
     * @returns {Error} The error object for the errorId passed in.
     */
    createErrorObject(errorId) {
        const error = new Error();
        error.setErrorId(errorId);
        return error;
    }

    /**
     * Sets the error state of this store so that components can react to it.
     * @param {TextUnitError} textUnitError
     */
    setErrorState(textUnitError) {
        this.errorsKeyedByTextUnitKey[textUnitError.textUnit.getTextUnitKey()] = textUnitError;
    }

    /**
     * Performs the review action on the textunit passed in as argument, or all the selected textunits
     * @param {ReviewTextUnitsDTO} reviewTextUnitsDTO
     */
    onReviewTextUnits(reviewTextUnitsDTO) {
        const { comment, textUnitAction } = reviewTextUnitsDTO;

        reviewTextUnitsDTO.textUnits.forEach(textUnit => {
            textUnit.setTargetComment(comment);

            // TODO clean up textUnitAction and refactor to enum.  These values are from: TextUnitsreviewModal
            switch (textUnitAction) {
                case "reject":
                    textUnit.setIncludedInLocalizedFile(false);
                    textUnit.setStatus(TextUnit.STATUS.TRANSLATION_NEEDED);
                    break;
                case "translate":
                    textUnit.setIncludedInLocalizedFile(true);
                    textUnit.setStatus(TextUnit.STATUS.TRANSLATION_NEEDED);
                    break;
                case "review":
                    textUnit.setIncludedInLocalizedFile(true);
                    textUnit.setStatus(TextUnit.STATUS.REVIEW_NEEDED);
                    break;
                case "accept":
                    textUnit.setIncludedInLocalizedFile(true);
                    textUnit.setStatus(TextUnit.STATUS.APPROVED);
                    break;
            }

            if (textUnit.getTarget() !== null) {
                // TODO: Test error handling when one or more saveTextUnit operations fail. How should they be handled in the UI?
                // TODO: Show visual clue on the workbench when save is successful.
                this.onSaveTextUnit(textUnit);
            }
        });
    }

    /**
     *
     * @param {TextUnit} textUnit
     * @return {TextUnitError}
     */
    static getError(textUnit) {
        return this.getState().errorsKeyedByTextUnitKey[textUnit.getTextUnitKey()];
    }
}

export default alt.createStore(TextUnitStore, 'TextUnitStore');
