import alt from "../../alt";

class ExportSearchResultsActions {

    constructor() {
        this.generateActions(
            "open",
            "close"
        );
    }
}

export default alt.createActions(ExportSearchResultsActions);
