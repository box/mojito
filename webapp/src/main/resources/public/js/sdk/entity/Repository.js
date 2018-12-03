import User from "./User";
import AssetIntegrityChecker from "./AssetIntegrityChecker";
import RepositoryLocale from "./RepositoryLocale";
import RepositoryStatistic from "./RepositoryStatistic";
import TM from "./TM";

export default class Repository {
    constructor() {

        /** @type {AssetIntegrityChecker[]} */
        this.assetIntegrityCheckers = null;

        /** @type {User} */
        this.createdByUser = null;

        /** @type {Date} */
        this.createdDate = null;

        /** @type {String} */
        this.description = "";

        /** @type {Boolean} */
        this.checkSLA = false;

        /** @type {String} */
        this.dropExporterType = "BOX";

        /** @type {Number} */
        this.id = 0;

        /** @type {Date} */
        this.lastModifiedDate = null;

        /** @type {String} */
        this.name = "";

        /** @type {RepositoryLocale[]} */
        this.repositoryLocales = null;

        /** @type {RepositoryStatistic} */
        this.repositoryStatistic = null;

        /** @type {TM} */
        this.tm = Object;
    }

    /**
     * Convert JSON User object
     *
     * @param {Object} json
     * @return {Repository}
     */
    static toRepository(json) {
        let result = new Repository();

        result.assetIntegrityCheckers = AssetIntegrityChecker.toAssetIntegrityCheckers(json.assetIntegrityCheckers);
        result.createdByUser = User.toUser(json.createdByUser);
        result.createdDate = new Date(json.createdDate);
        result.description = json.description;
        result.checkSLA = json.checkSLA;
        result.dropExporterType = json.dropExporterType;
        result.id = json.id;
        result.lastModifiedDate = new Date(json.lastModifiedDate);
        result.name = json.name;
        result.repositoryLocales = RepositoryLocale.toRepositoryLocales(json.repositoryLocales);
        result.repositoryStatistic = RepositoryStatistic.toRepositoryStatistic(json.repositoryStatistic);
        result.tm = TM.toTM(json.tm);

        return result;
    }

    /**
     * @param {Object[]} jsons
     * @return {Repository[]}
     */
    static toRepositorys(jsons) {
        let results = [];

        for (let json of jsons) {
            results.push(Repository.toRepository(json));
        }

        return results;
    }
}
