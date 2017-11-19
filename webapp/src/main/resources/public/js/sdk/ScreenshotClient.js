import BaseClient from "./BaseClient";

class ScreenshotClient extends BaseClient {

    /**
     * Gets the text units that matches the searcher parameters.
     *
     * @param {TextUnitSearcherParameters} textUnitSearcherParameters
     *
     * @returns {Promise.<TextUnit[]|err>} a promise that retuns an array of text units
     */
    getScreenshots(params) {
        return this.get(this.getUrl(), params);
    }
 
    updateScreenshot(screenshot) {
        return this.put(this.getUrl() + '/' + screenshot.id, screenshot);
    }

    getEntityName() {
        return 'screenshots';
    }
}
;

export default new ScreenshotClient();



