import alt from "../../alt";

import PageRequestResults from "../../sdk/PageRequestResults"
import DropActions from "../../actions/drop/dropActions";
import DropDataSource from "./DropDataSource";

class DropStore {

    constructor() {

        /** @type {Drop} */
        this.onGoingDrops = [];
        /** @type {Number} */
        this.onGoingCurrentPageNumber;
        /** @type {Boolean} */
        this.noMoreOnGoingRequestResults;  
        /** @type {Drop} */
        this.importedDrops = [];
        /** @type {Number} */
        this.importedCurrentPageNumber;
        /** @type {Boolean} */
        this.hasMoreImportedRequestResults;

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
        this.onGoingDrops = pageRequestResults.results;
        this.onGoingCurrentPageNumber = pageRequestResults.currentPageNumber;
        this.hasMoreOnGoingRequestResults = pageRequestResults.hasMoreResults;
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
        this.importedDrops = pageRequestResults.results;
        this.importedCurrentPageNumber = pageRequestResults.currentPageNumber;
        this.hasMoreImportedRequestResults = pageRequestResults.hasMoreResults;
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
