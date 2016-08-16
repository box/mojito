export default class AssetIntegrityChecker {
    constructor() {

        /** @type {String} */
        this.assetExtension = "";

        /** @type {Number} */
        this.id = 0;

        /** @type {String} */
        this.integrityCheckerType = "";
    }

    /**
     * @param {Object} json
     * @return {AssetIntegrityChecker}
     */
    static toAssetIntegrityChecker(json) {
        let result = new AssetIntegrityChecker();

        result.assetExtension = json.assetExtension;
        result.id = json.id;
        result.integrityCheckerType = json.integrityCheckerType;

        return result;
    }

    /**
     * @param {Object[]} jsons
     * @return {AssetIntegrityChecker[]}
     */
    static toAssetIntegrityCheckers(jsons) {
        let results = null;
        if (jsons) {
            results = [];
            for (let json of jsons) {
                results.push(AssetIntegrityChecker.toAssetIntegrityChecker(json));
            }
        }
        return results;
    }
}
