import alt from "../../alt.js";

import DropActions from "../../actions/drop/dropActions.js";
import DropDataSource from "./DropDataSource.js";

class DropStore {

    constructor() {
        /** @type {Drop[]} */
        this.drops = [];

        /** @type {Number} */
        this.currentPageNumber = 0;

        /** @type {Boolean} */
        this.hasMoreDrops = false;

        this.bindActions(DropActions);

        this.registerAsync(DropDataSource);
    }

    /**
     * @param {PageRequestParams} pageRequestParams
     */
    onGetAllInProcess(pageRequestParams) {
        this.getInstance().getAllInProcess(pageRequestParams);
    }

    /**
     * @param {PageRequestResults} pageRequestResults
     */
    onGetAllInProcessSuccess(pageRequestResults) {
        this.drops = pageRequestResults.results;
        this.currentPageNumber = pageRequestResults.currentPageNumber;
        this.hasMoreDrops = pageRequestResults.hasMoreResults;
    }

    /**
     * @param {PageRequestParams} pageRequestParams
     */
    onGetAllImported(pageRequestParams) {
        this.getInstance().getAllImported(pageRequestParams);
    }

    /**
     * @param {PageRequestResults} pageRequestResults
     */
    onGetAllImportedSuccess(pageRequestResults) {
        this.drops = pageRequestResults.results;
        this.currentPageNumber = pageRequestResults.currentPageNumber;
        this.hasMoreDrops = pageRequestResults.hasMoreResults;
    }

    /**
     * @param {PageRequestParams} pageRequestParams
     */
    onGetAll(pageRequestParams) {
        this.getInstance().getAll(pageRequestParams);
    }

    /**
     * @param {PageRequestResults} pageRequestResults
     */
    onGetAllSuccess(pageRequestResults) {
        this.drops = pageRequestResults.results;
        this.currentPageNumber = pageRequestResults.currentPageNumber;
        this.hasMoreDrops = pageRequestResults.hasMoreResults;
    }

    /**
     * @param {ExportDropConfig} exportDropConfig
     */
    onCreateNewRequest(exportDropConfig) {
        this.getInstance().createNewRequest(exportDropConfig);
    }

    /**
     * @param {ImportDropConfig} importDropConfig
     */
    onImportRequest(importDropConfig) {
        this.getInstance().importRequest(importDropConfig);
    }

    /**
     * @param {number} dropId
     */
    onCompleteRequest(dropId) {
        this.getInstance().completeRequest(dropId);
    }

    /**
     * @param {CancelDropConfig} cancelDropConfig
     */
    onCancelRequest(cancelDropConfig) {
        this.getInstance().cancelRequest(cancelDropConfig);
    }
}

export default alt.createStore(DropStore, 'DropStore');
