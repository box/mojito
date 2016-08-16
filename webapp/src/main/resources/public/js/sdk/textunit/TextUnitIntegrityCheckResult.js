export default class TextUnitIntegrityCheckResult {
    constructor(data = null) {
        /** @type {boolean} */
        this.checkResult = true;

        /** @type {String} */
        this.failureDetail = "";

        if (data) {
            this.checkResult = data.checkResult;
            this.failureDetail = data.failureDetail;
        }
    }
}
