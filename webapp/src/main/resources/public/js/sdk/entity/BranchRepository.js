import SourceLocale from "./SourceLocale";
import ManualScreenshotRun from "./ManualScreenshotRun";

export default class BranchRepository {
    constructor() {

        /** @type {Number} */
        this.id = 0;

        /** @type {String} */
        this.name = "";

        /** @type {SourceLocale} */
        this.sourceLocale = null;

        /** @type {manualScreenshotRun} */
        this.manualScreenshotRun = null;
    }

    /**
     * Convert JSON User object
     *
     * @param {Object} json
     * @return {Repository}
     */
    static toBranchRepository(json) {
        let result = new BranchRepository();

        result.id = json.id;
        result.name = json.name;
        result.sourceLocale = SourceLocale.toSourceLocale(json.sourceLocale);
        result.manualScreenshotRun = ManualScreenshotRun.toManualScreenshotRun(json.manualScreenshotRun);

        return result;
    }
}