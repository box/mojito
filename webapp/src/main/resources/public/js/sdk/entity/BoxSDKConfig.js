export default class BoxSDKConfig {
    constructor() {

        /** @type {string} */
        this.clientId = "";

        /** @type {string} */
        this.clientSecret = "";

        /** @type {string} */
        this.publicKeyId = "";

        /** @type {string} */
        this.privateKey = "";

        /** @type {string} */
        this.privateKeyPassword = "";

        /** @type {string} */
        this.enterpriseId = "";

        /** @type {string} */
        this.appUserId = "";

        /** @type {string} */
        this.rootFolderId = "";

        /** @type {string} */
        this.dropsFolderId = "";

        /** @type {boolean} */
        this.bootstrap = true;

        /** @type {boolean} */
        this.validated = false;

        /** @type {string} */
        this.rootFolderUrl = "";
    }

    /**
     * @param {Object} json
     * @return {BoxSDKConfig}
     */
    static toBoxSDKConfig(json) {
        let result = null;

        if (json) {
            result = new BoxSDKConfig();

            result.clientId = json.clientId;
            result.clientSecret = json.clientSecret;
            result.publicKeyId = json.publicKeyId;
            result.privateKey = json.privateKey;
            result.privateKeyPassword = json.privateKeyPassword;
            result.enterpriseId = json.enterpriseId;
            result.appUserId = json.appUserId;
            result.rootFolderId = json.rootFolderId;
            result.dropsFolderId = json.dropsFolderId;
            result.bootstrap = json.bootstrap;
            result.validated = json.validated;
            result.rootFolderUrl = json.rootFolderUrl;
        }

        return result;
    }

    /**
     * @param {Object[]} jsons
     * @return {BoxSDKConfig[]}
     */
    static toBoxSDKConfigs(jsons) {
        let results = [];

        for (let json of jsons) {
            results.push(BoxSDKConfig.toBoxSDKConfig(json));
        }

        return results;
    }
}
