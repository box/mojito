import TmTextUnit from "./TmTextUnit";


export default class BranchStatisticScreenshot {
    constructor() {

        /** @type {Number} */
        this.id = null;
        this.src = null;
        this.textUnits = [];

    }

    /**
     * Convert JSON User object
     *
     * @param {Object} json
     * @return {Repository}
     */
    static toBranchStatisticScreenshot(json) {
        const result = new BranchStatisticScreenshot();
        if (json) {
            result.id = json.id;
            result.src = json.src;
            result.textUnits = TextUnit.toTextUnits(json.textUnits);
        }

        return result;
    }

    static toBranchStatisticScreenshotList(jsons) {
        const results = [];

        if (jsons && jsons.length > 0) {
            for (const json of jsons) {
                results.push(this.toBranchStatisticScreenshot(json));
            }
        }

        return results;
    }
}

class TextUnit {

    constructor() {
        this.id = null;
        this.tmTextUnit = null;
    }

    static toTextUnit(json) {
        const result = new TextUnit();
        if (json) {
            result.id = json.id;
            result.tmTextUnit = TmTextUnit.toTmTextUnit(json.tmTextUnit);
        }

        return result;
    }

    static toTextUnits(jsons) {
        const result = [];

        if (jsons && jsons.length > 0) {
            for (const json of jsons) {
                result.push(TextUnit.toTextUnit(json));
            }
        }

        return result;
    }


}