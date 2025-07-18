import Locale from "./Locale.js";

export default class RepositoryLocale {
    constructor() {

        /** @type {Locale} */
        this.locale = null;

        /** @type {RepositoryLocale} */
        this.parentLocale = null;

        /** @type {Boolean} */
        this.toBeFullyTranslated = true;
    }

    /**
     * @param {Object} json
     * @return {RepositoryLocale}
     */
    static toRepositoryLocale(json) {
        const result = new RepositoryLocale();

        result.locale = Locale.toLocale(json.locale);

        if (json.parentLocale) {
            result.parentLocale = RepositoryLocale.toRepositoryLocale(json.parentLocale);
        }

        result.toBeFullyTranslated = json.toBeFullyTranslated;

        return result;
    }

    /**
     * @param {Object[]} jsons
     * @return {RepositoryLocale[]}
     */
    static toRepositoryLocales(jsons) {
        let results = null;
        if (jsons) {
            results = [];
            for (const json of jsons) {
                results.push(RepositoryLocale.toRepositoryLocale(json));
            }
        }
        return results;
    }

    /**
     * Check to see if the repositoryLocale is the root locale
     *
     * @param repositoryLocale
     * @return {boolean}
     */
    static isRootLocale(repositoryLocale) {
        // eslint-disable-next-line eqeqeq
        return (repositoryLocale.parentLocale == null);
    }
}
