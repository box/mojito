import User from "./User";
import RepositoryLocaleStatistic from "./RepositoryLocaleStatistic";

export default class RepositoryStatistic {
    constructor() {

        /** @type {User} */
        this.createdByUser = null;

        /** @type {Date} */
        this.createdDate = null;

        /** @type {Date} */
        this.lastModifiedDate = null;

        /** @type {RepositoryLocaleStatistic|null} */
        this.repositoryLocaleStatistics = null;

        /** @type {Number} */
        this.uncommentedTextUnitCount = 0;

        /** @type {Number} */
        this.unusedTextUnitCount = 0;

        /** @type {Number} */
        this.unusedTextUnitWordCount = 0;

        /** @type {Number} */
        this.usedTextUnitCount = 0;

        /** @type {Number} */
        this.usedTextUnitWordCount = 0;
    }

    /**
     * @param {Object} json
     * @return {RepositoryStatistic}
     */
    static toRepositoryStatistic(json) {
        let result = null;
        if (json) {
            result = new RepositoryStatistic();
            
            this.createdByUser = User.toUser(json.createdByUser);
            this.createdDate = new Date(json.createdDate);
            this.lastModifiedDate = new Date(json.lastModifiedDate);

            if (json.repositoryLocaleStatistics) {
                this.repositoryLocaleStatistics = RepositoryLocaleStatistic.toRepositoryLocaleStatistics(json.repositoryLocaleStatistics);
            }

            this.uncommentedTextUnitCount = json.uncommentedTextUnitCount;
            this.unusedTextUnitCount = json.unusedTextUnitCount;
            this.unusedTextUnitWordCount = json.unusedTextUnitWordCount;
            this.usedTextUnitCount = json.usedTextUnitCount;
            this.usedTextUnitWordCount = json.usedTextUnitWordCount;
        }
        return result;
    }

    /**
     * @param {Object[]} jsons
     * @return {RepositoryStatistic[]}
     */
    static toRepositoryStatistics(jsons) {
        let results = [];

        for (let json of jsons) {
            results.push(RepositoryStatistic.toRepositoryStatistic(json));
        }

        return results;
    }
}
