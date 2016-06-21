import DropClient from "../../sdk/DropClient";
import PageRequestResults from "../../sdk/PageRequestResults";
import DropActions from "../../actions/drop/dropActions";

const DropDataSource = {

    getAllInProcess: {
        remote(dropStoreState, pageRequestParams) {
            return DropClient.getDrops(null, false, pageRequestParams.page, pageRequestParams.size).then(function (results) {
                return new PageRequestResults(results, pageRequestParams.page, results.length == pageRequestParams.size);
            });
        },
        success: DropActions.getAllInProcessSuccess,
        error: DropActions.getAllInProcessError
    },
    
    getAllImported: {
        remote(dropStoreState, pageRequestParams) {
            return DropClient.getDrops(null, true, pageRequestParams.page, pageRequestParams.size).then(function (results) {
                return new PageRequestResults(results, pageRequestParams.page, results.length == pageRequestParams.size);
            });
        },
        success: DropActions.getAllImportedSuccess,
        error: DropActions.getAllImportedError
    },

    getAll: {
        remote(dropStoreState, pageRequestParams) {
            return DropClient.getDrops(null, null, pageRequestParams.page, pageRequestParams.size).then(function (results) {
                return new PageRequestResults(results, pageRequestParams.page, results.length == pageRequestParams.size);
            });
        },
        success: DropActions.getAllSuccess,
        error: DropActions.getAllError
    },

    createNewRequest: {
        remote(dropStoreState, exportDropConfig) {
            return DropClient.exportDrop(exportDropConfig).then(function (results) {
                return results;
            });
        },
        success: DropActions.createNewRequestSuccess,
        error: DropActions.createNewRequestError
    },

    importRequest: {
        remote(dropStoreState, importDropConfig) {
            return DropClient.importDrop(importDropConfig).then(function (results) {
                return results;
            });
        },
        success: DropActions.importRequestSuccess,
        error: DropActions.importRequestError
    },

    cancelRequest: {
        remote(dropStoreState, cancelDropConfig) {
            return DropClient.cancelDrop(cancelDropConfig).then(function (results) {
                return results;
            });
        },
        success: DropActions.cancelRequestSuccess,
        error: DropActions.cancelRequestError
    },

};

export default DropDataSource;
