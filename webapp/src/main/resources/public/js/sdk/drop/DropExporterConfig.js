export default
class DropExporterConfig {
    constructor() {
        /** @type {Number} */
        this.dropFolderId = 0;

        /** @type {Number} */
        this.importedFolderId = 0;

        /** @type {Number} */
        this.localizedFolderId = 0;

        /** @type {Number} */
        this.queriesFolderId = 0;

        /** @type {Number} */
        this.quotesFolderId = 0;

        /** @type {Number} */
        this.sourceFolderId = 0;

        /** @type {Date} */
        this.uploadDate = null;
    }

    /**
     * Convert JSON DropExporterConfig object
     *
     * @param {Object} json
     * @return {DropExporterConfig}
     */
    static toDropExporterConfig(json) {
        let result = new DropExporterConfig();

        result.dropFolderId = json.dropFolderId;
        result.importedFolderId = json.importedFolderId;
        result.localizedFolderId = json.localizedFolderId;
        result.queriesFolderId = json.queriesFolderId;
        result.quotesFolderId = json.quotesFolderId;
        result.sourceFolderId = json.sourceFolderId;
        result.uploadDate = json.uploadDate;

        return result;
    }

    /**
     *
     * @param {Object[]} jsons
     * @return {DropExporterConfig[]}
     */
    static toDropExporterConfigs(jsons) {

        var results = [];

        for (let DropExporterConfig of jsons) {
            results.push(DropExporterConfig.toDropExporterConfig(DropExporterConfig));
        }

        return results;
    }
}
