import alt from "../../alt.js";

class BranchTextUnitsPageActions {
    constructor() {
        this.generateActions(
            "getBranchTextUnits",
            "resetBranchTextUnitsSearchParams",
            "getBranchTextUnitsSuccess",
            "getBranchTextUnitsError",
        );
    }
}

export default alt.createActions(BranchTextUnitsPageActions);
