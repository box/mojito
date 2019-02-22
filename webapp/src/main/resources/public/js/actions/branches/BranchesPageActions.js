import alt from "../../alt";

class BranchesPageActions {

    constructor() {
        this.generateActions(
            "getBranches",
            "getBranchesSuccess",
            "getBranchesError",
            "changeOpenBranchStatistic",
            "changeSelectedBranchTextUnitIds",
            "resetBranchesSearchParams",
        );
    }
}

export default alt.createActions(BranchesPageActions);