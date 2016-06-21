import alt from "../../alt";

import PageRequestResults from "../../sdk/PageRequestResults"
import DropActions from "../../actions/drop/dropActions";
import DropDataSource from "./DropDataSource";

class DropStore {

    constructor() {
        /** @type {Drop[]} */
        this.drops = [];

        /** @type {Number} */
        this.currentPageNumber = 0;

        /** @type {Boolean} */
        this.hasMoreDrops = true;


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
     * @param {ExportDropConfig} exportDropConfig
     */
    onCreateNewRequest(exportDropConfig) {
        this.getInstance().createNewRequest(exportDropConfig);
    }

    /**
     * @param {CancelDropConfig} importDropConfig
     */
    onImportRequest(importDropConfig) {
        this.getInstance().importRequest(importDropConfig);
    }

    /**
     * @param {CancelDropConfig} cancelDropConfig
     */
    onCancelRequest(cancelDropConfig) {
        this.getInstance().cancelRequest(cancelDropConfig);
    }
}

export default alt.createStore(DropStore, 'DropStore');
