import TmTextUnit from "./TmTextUnit"

export default class BranchTextUnitStatistics {
    constructor() {
        /**
         *
         * @type {BigInt}
         */
        this.id = null;

        /**
         *
         * @type {TmTextUnit}
         */
        this.tmTextUnit = null;

        /**
         *
         * @type {BigInt}
         */
        this.forTranslationCount = null;

        /**
         *
         * @type {BigInt}
         */
        this.totalCount = null;
    }

    static toBranchTextUnitStatistics(json) {
        let result = new BranchTextUnitStatistics();

        if (json) {
            result.id = json.id;
            result.tmTextUnit = TmTextUnit.toTmTextUnit(json.tmTextUnit);
            result.forTranslationCount = json.forTranslationCount;
            result.totalCount = json.totalCount;
        }

        return result;
    }

    static toBranchTextUnitStatisticsList(jsons) {
        let result = [];

        if (jsons && jsons.length > 0) {
            for (let json of jsons) {
                result.push(BranchTextUnitStatistics.toBranchTextUnitStatistics(json));
            }
        }

        return result;
    }

}