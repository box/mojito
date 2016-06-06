import BaseClient from './BaseClient';
import Drop from './drop/Drop';
import ExportDropConfig from './drop/ExportDropConfig';

class DropClient extends BaseClient {

    /**
     * @param {int} repoId
     * @param {boolean} isImported
     * @param {int} page starts at index 0
     * @param {int} size
     * @return {*}
     */
    getDrops(repoId = null, isImported = null, page = 0, size = 10) {

        let promise = this.get(this.getUrl(), {
            "repositoryId": repoId,
            "imported": isImported,           
            "page": page,
            "size": size
        });

        return promise.then(function (result) {
            return Drop.toDrops(result);
        });
    }

    /**
     *
     * @param {ExportDropConfig} exportDropConfig
     * @return {Promise}
     */
    exportDrop(exportDropConfig) {
        let promise = this.post(this.getUrl() + "/export", exportDropConfig);
        return promise.then((result) => result);
    }

    /**
     * @param {CancelDropConfig} importDropConfig
     * @return {Promise}
     */
    importDrop(importDropConfig) {
        let promise = this.post(this.getUrl() + "/import", importDropConfig);
        return promise.then((result) => result);
    }

    /**
     * @param {CancelDropConfig} cancelDropConfig
     * @return {Promise}
     */
    cancelDrop(cancelDropConfig) {
        let promise = this.post(this.getUrl() + "/cancel", cancelDropConfig);
        return promise.then((result) => result);
    }

    /**
     * @return {string}
     */
    getEntityName() {
        return 'drops';
    }
}

export default new DropClient();



