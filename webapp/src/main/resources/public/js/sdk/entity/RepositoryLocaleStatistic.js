import Locale from "./Locale";

export default class RepositoryLocaleStatistic {
    constructor() {

        /** @type {Locale} */
        this.locale = null;

        /** @type {Number} */
        this.translatedCount = 0;

        /** @type {Number} */
        this.translatedWordCount = 0;

        /** @type {Number} */
        this.translationNeededCount = 0;

        /** @type {Number} */
        this.translationNeededWordCount = 0;

        /** @type {Number} */
        this.reviewNeededCount = 0;

        /** @type {Number} */
        this.reviewNeededWordCount = 0

        /** @type {Number} */
        this.includeInFileCount = 0;

        /** @type {Number} */
        this.includeInFileWordCount = 0;
    }

    /**
     * @param {Object} json
     * @return {RepositoryLocaleStatistic}
     */
    static toRepositoryLocaleStatistic(json) {
        let result = new RepositoryLocaleStatistic();

        result.locale = Locale.toLocale(json.locale);
        result.translatedCount = json.translatedCount;
        result.translatedWordCount = json.translatedWordCount;
        result.translationNeededCount = json.translationNeededCount;
        result.translationNeededWordCount = json.translationNeededWordCount;
        result.reviewNeededCount = json.reviewNeededCount;
        result.reviewNeededWordCount = json.reviewNeededWordCount;
        result.includeInFileCount = json.includeInFileCount;
        result.includeInFileWordCount = json.includeInFileWordCount;

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
