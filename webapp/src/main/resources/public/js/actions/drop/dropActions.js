import alt from "../../alt";

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
            "cancelRequest",
            "cancelRequestSuccess",
            "cancelRequestError"
        );
    }
}

export default alt.createActions(dropActions);
