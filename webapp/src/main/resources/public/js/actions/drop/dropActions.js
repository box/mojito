import alt from "../../alt.js";

class dropActions {

    constructor() {
        this.generateActions(
            "getAllInProcess",
            "getAllInProcessSuccess",
            "getAllInProcessError",
            "getAllImported",
            "getAllImportedSuccess",
            "getAllImportedError",
            "getAll",
            "getAllSuccess",
            "getAllError",
            "createNewRequest",
            "createNewRequestSuccess",
            "createNewRequestError",
            "importRequest",
            "importRequestSuccess",
            "importRequestError",
            "completeRequest",
            "completeRequestSuccess",
            "completeRequestError",
            "cancelRequest",
            "cancelRequestSuccess",
            "cancelRequestError"
        );
    }
}

export default alt.createActions(dropActions);
