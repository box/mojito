import alt from "../../alt";

class BranchesHistoryActions {

    constructor() {
        this.generateActions(
            "enableHistoryUpdate",
            "disableHistoryUpdate",
        );
    }
}

export default alt.createActions(BranchesHistoryActions);
