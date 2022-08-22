import Branch from "./Branch"
import BranchTextUnitStatistics from "./BranchTextUnitStatistics";
const totalCountLte = 30000;

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
         * @type {BranchTextUnitStatistics[]}
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

        /**
         *
         * @type {boolean}
         */
        this.isTruncated = false;

    }

    static toContent(json) {
        let result = new BranchStatisticsContent();
        if (json) {
            result.id = json.id;
            result.branch = Branch.toBranch(json.branch);
            result.forTranslationCount = json.forTranslationCount;
            result.totalCount = json.totalCount;

            if (result.totalCount > totalCountLte){
                result.isTruncated = true;
            }
            else {
                result.branchTextUnitStatistics = BranchTextUnitStatistics.toBranchTextUnitStatisticsList(json.branchTextUnitStatistics);
            }
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