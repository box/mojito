import BaseClient from "./BaseClient";
import TextUnit from "./TextUnit";
import TextUnitIntegrityCheckRequest from "./textunit/TextUnitIntegrityCheckRequest";
import TextUnitIntegrityCheckResult from "./textunit/TextUnitIntegrityCheckResult";
import PollableTaskClient from "./PollableTaskClient";


class TextUnitClient extends BaseClient {

    /**
     * Gets the text units that matches the searcher parameters.
     *
     * @param {TextUnitSearcherParameters} textUnitSearcherParameters
     *
     * @returns {Promise.<TextUnit[]|err>} a promise that retuns an array of text units
     */
    getTextUnits(textUnitSearcherParameters) {

        let promise = this.get(this.getUrl(), textUnitSearcherParameters.getParams());

        return promise.then(function (result) {
            return TextUnit.toTextUnits(result);
        });
    }

    /**
     * Deletes the current text unit.
     *
     * @param {TextUnit} textUnit
     * @returns {Promise}
     */
    deleteCurrentTranslation(textUnit) {
        return this.delete(this.getUrl(textUnit.getTmTextUnitCurrentVariantId())).then(function () {
            textUnit.setTarget(null);
            textUnit.setTranslated(false);
            return textUnit;
        });
    }

    /**
     * Saves a TextUnit.
     *
     * @param {TextUnit} textUnit
     * @returns {Promise<TextUnit, err>} a promise that returns the updated or created text unit
     */
    saveTextUnit(textUnit) {
        return this.post(this.getUrl(), textUnit.data).then(function (jsonTextUnit) {
            return TextUnit.toTextUnit(jsonTextUnit);
        });
    }

    /**
     * Saves a TextUnit.
     *
     * @param {TextUnit} textUnit
     * @returns {Promise<TextUnitIntegrityCheckResult, err>} a promise that returns the updated or created text unit
     */
    checkTextUnitIntegrity(textUnit) {
        let request = new TextUnitIntegrityCheckRequest();
        request.contentToCheck = textUnit.getTarget();
        request.textUnitId = textUnit.getTmTextUnitId();

        return this.get(this.getUrl() + '/check', request).then(function (jsonTextUnit) {
            return new TextUnitIntegrityCheckResult(jsonTextUnit);
        });
    }

    /**
     * Saves a VirtualAssetTextUnit build of TextUnit information.
     *
     * @param {TextUnit} textUnit
     * @returns
     */
    saveVirtualAssetTextUnit(textUnit) {
        return this.post(this.getAssetTextUnitsUrl(textUnit.getAssetId()), [{
            name: textUnit.getName(),
            content: textUnit.getSource(),
            comment: textUnit.getComment(),
            pluralForm : textUnit.getPluralForm(),
            pluralFormOther: textUnit.getPluralFormOther(),
            doNotTranslate: textUnit.getDoNotTranslate(),
        }]).then(function(pollableTask) {
            return PollableTaskClient.waitForPollableTaskToFinish(pollableTask.id).then(function(pollableTask) {
                if (pollableTask.errorMessage) {
                    throw new Error(pollableTask.errorMessage);
                }

                return textUnit;
            });
        });
    }

    getAssetTextUnitsUrl(assetId) {
        return this.baseUrl + 'virtualAssets/' + assetId + '/textUnits';
    }


    getEntityName() {
        return 'textunits';
    }
}
;

export default new TextUnitClient();



