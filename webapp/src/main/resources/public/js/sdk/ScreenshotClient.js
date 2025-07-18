import BaseClient from "./BaseClient.js";

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

    createOrUpdateScreenshotRun(screenshotRun) {
        return this.post(this.getUrl(), screenshotRun);
    }

    getEntityName() {
        return 'screenshots';
    }

    deleteScreenshot(screenshotId) {
        return this.delete(`${this.getUrl()}/${screenshotId}`);
    }
}


export default new ScreenshotClient();



