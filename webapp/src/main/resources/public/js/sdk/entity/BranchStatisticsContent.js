import Branch from "./Branch"
import BranchTextUnitStatistics from "./BranchTextUnitStatistics";

export default class BranchStatisticsContent {
    constructor() {

        /**
         *
         * @type {BigInt}
         */
        this.id = null;

        /**
         *
         * @type {Branch}
         */
        this.branch = null;

        /**
         *
         * @type {branchStatisticsContent[]}
         */
        this.branchTextUnitStatistics = [];

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

    static toContent(json) {
        let result = new BranchStatisticsContent();

        if (json) {
            result.id = json.id;
            result.branch = Branch.toBranch(json.branch);
            result.branchTextUnitStatistics = BranchTextUnitStatistics.toBranchTextUnitStatisticsList(json.branchTextUnitStatistics);
            result.forTranslationCount = json.forTranslationCount;
            result.totalCount = json.totalCount;
        }


        return result;
    }

    /**
     * @param {Object[]} jsons
     * @return {RepositoryLocaleStatistic[]}
     */
    static toContentList(jsons) {
        let results = [];

        if (jsons && jsons.length > 0) {
            for (let json of jsons) {
                results.push(this.toContent(json));
            }
        }

        return results;
    }
}