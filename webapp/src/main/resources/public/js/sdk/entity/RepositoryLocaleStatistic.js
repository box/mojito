import Locale from "./Locale";

export default class RepositoryLocaleStatistic {
    constructor() {

        /** @type {Locale} */
        this.locale = null;

        /** @type {Number} */
        this.translatedCount = 0;

        /** @type {Number} */
        this.translationNeededCount = 0;

        /** @type {Number} */
        this.reviewNeededCount = 0;

        /** @type {Number} */
        this.includeInFileCount = 0;
    }

    /**
     * @param {Object} json
     * @return {RepositoryLocaleStatistic}
     */
    static toRepositoryLocaleStatistic(json) {
        let result = new RepositoryLocaleStatistic();

        result.locale = Locale.toLocale(json.locale);
        result.translatedCount = json.translatedCount;
        result.translationNeededCount = json.translationNeededCount;
        result.reviewNeededCount = json.reviewNeededCount;
        result.includeInFileCount = json.includeInFileCount;

        return result;
    }

    /**
     * @param {Object[]} jsons
     * @return {RepositoryLocaleStatistic[]}
     */
    static toRepositoryLocaleStatistics(jsons) {
        let results = [];

        for (let json of jsons) {
            results.push(RepositoryLocaleStatistic.toRepositoryLocaleStatistic(json));
        }

        return results;
    }
}
