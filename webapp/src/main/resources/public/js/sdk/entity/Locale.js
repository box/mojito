export default class Locale {
    constructor() {

        /** @type {Number} */
        this.id = 0;

        /** @type {String} */
        this.bcp47Tag = "";
    }

    /**
     * @param {Object} json
     * @return {Locale}
     */
    static toLocale(json) {
        const result = new Locale();

        result.id = 1;
        result.bcp47Tag = json.bcp47Tag;

        return result;
    }

    /**
     * @param {Object[]} jsons
     * @return {Locale[]}
     */
    static toLocales(jsons) {
        const results = [];

        for (const json of jsons) {
            results.push(Locale.toLocale(json));
        }

        return results;
    }
}
