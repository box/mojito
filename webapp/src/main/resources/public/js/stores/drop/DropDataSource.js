import DropClient from "../../sdk/DropClient.js";
import DropActions from "../../actions/drop/dropActions.js";

const DropDataSource = {

    getAllInProcess: {
        remote(dropStoreState, pageRequestParams) {
            return DropClient.getDrops(null, false, false, pageRequestParams.page, pageRequestParams.size);
        },
        success: DropActions.getAllInProcessSuccess,
        error: DropActions.getAllInProcessError
    },

    getAllImported: {
        remote(dropStoreState, pageRequestParams) {
            return DropClient.getDrops(null, true, false, pageRequestParams.page, pageRequestParams.size);
        },
        success: DropActions.getAllImportedSuccess,
        error: DropActions.getAllImportedError
    },

    getAll: {
        remote(dropStoreState, pageRequestParams) {
            return DropClient.getDrops(null, null, null, pageRequestParams.page, pageRequestParams.size);
        },
        success: DropActions.getAllSuccess,
        error: DropActions.getAllError
    },

    createNewRequest: {
        remote(dropStoreState, exportDropConfig) {
            return DropClient.exportDrop(exportDropConfig);
        },
        success: DropActions.createNewRequestSuccess,
        error: DropActions.createNewRequestError
    },

    importRequest: {
        remote(dropStoreState, importDropConfig) {
            return DropClient.importDrop(importDropConfig);
        },
        success: DropActions.importRequestSuccess,
        error: DropActions.importRequestError
    },

    completeRequest: {
        remote(dropStoreState, dropId) {
            return DropClient.completeDrop(dropId);
        },
        success: DropActions.completeRequestSuccess,
        error: DropActions.completeRequestError
    },

    cancelRequest: {
        remote(dropStoreState, cancelDropConfig) {
            return DropClient.cancelDrop(cancelDropConfig);
        },
        success: DropActions.cancelRequestSuccess,
        error: DropActions.cancelRequestError
    },

};

export default DropDataSource;
