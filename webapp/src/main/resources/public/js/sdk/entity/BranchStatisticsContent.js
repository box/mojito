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
         * @type {number}
         */
        this.textUnitTotalCount = 0;

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
        this.isPaginated = false;

    }

    static toContent(json) {
        let result = new BranchStatisticsContent();
        if (json) {
            result.id = json.id;
            result.branch = Branch.toBranch(json.branch);
            result.forTranslationCount = json.forTranslationCount;
            result.totalCount = json.totalCount;
            const branchTextUnitStatistics =
                BranchTextUnitStatistics.toBranchTextUnitStatisticsList(json.branchTextUnitStatistics);
            result.textUnitTotalCount = branchTextUnitStatistics.length;
            if (result.totalCount > totalCountLte){
                result.isPaginated = true;
            }
            else {
                result.branchTextUnitStatistics = branchTextUnitStatistics;
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