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
        let result = new Locale();

        result.id = 1;
        result.bcp47Tag = json.bcp47Tag;

        return result;
    }

    /**
     * @param {Object[]} jsons
     * @return {Locale[]}
     */
    static toLocales(jsons) {
        let results = [];

        for (let json of jsons) {
            results.push(Locale.toLocale(json));
        }

        return results;
    }
}
