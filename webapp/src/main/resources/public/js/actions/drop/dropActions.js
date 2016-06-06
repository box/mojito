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
