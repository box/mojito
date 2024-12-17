import BaseClient from './BaseClient';
import Drop from './drop/Drop';
import PageRequestResults from "./PageRequestResults";

class DropClient extends BaseClient {

    /**
     * @param {int|null} repoId
     * @param {boolean|null} isImported
     * @param {int} page starts at index 0
     * @param {int} size
     * @return {*}
     */
    getDrops(repoId = null, isImported = null, isCanceled = null, page = 0, size = 10) {

        const promise = this.get(this.getUrl(), {
            "repositoryId": repoId,
            "imported": isImported,
            "canceled": isCanceled,
            "page": page,
            "size": size
        });

        return promise.then(function (result) {

            const pageRequestResults = new PageRequestResults();

            pageRequestResults.results = Drop.toDrops(result.content);
            pageRequestResults.hasMoreResults = result.hasNext;
            pageRequestResults.currentPageNumber = result.number;

            return pageRequestResults;
        });
    }

    /**
     *
     * @param {ExportDropConfig} exportDropConfig
     * @return {Promise}
     */
    exportDrop(exportDropConfig) {
        const promise = this.post(this.getUrl() + "/export", exportDropConfig);
        return promise.then((result) => result);
    }

    /**
     * @param {ImportDropConfig} importDropConfig
     * @return {Promise}
     */
    importDrop(importDropConfig) {
        const promise = this.post(this.getUrl() + "/import", importDropConfig);
        return promise.then((result) => result);
    }

    /**
     * @param {number} dropId
     * @return {Promise}
     */
    completeDrop(dropId) {
        const promise = this.post(this.getUrl() + "/complete/" + dropId);
        return promise.then((result) => result);
    }

    /**
     * @param {CancelDropConfig} cancelDropConfig
     * @return {Promise}
     */
    cancelDrop(cancelDropConfig) {
        const promise = this.post(this.getUrl() + "/cancel", cancelDropConfig);
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



