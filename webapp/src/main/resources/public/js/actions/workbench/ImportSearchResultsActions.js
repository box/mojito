import alt from "../../alt";

class ImportSearchResultsActions {

    constructor() {
        this.generateActions(
            "open",
            "close"
        );
    }
}

export default alt.createActions(ImportSearchResultsActions);

