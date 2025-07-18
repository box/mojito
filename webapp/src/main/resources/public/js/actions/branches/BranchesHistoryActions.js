import alt from "../../alt.js";

class BranchesHistoryActions {

    constructor() {
        this.generateActions(
            "enableHistoryUpdate",
            "disableHistoryUpdate",
        );
    }
}

export default alt.createActions(BranchesHistoryActions);
