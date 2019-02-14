import alt from "../../alt";

class DashboardPageActions {

    constructor() {
        this.generateActions(
            "getBranches",
            "getBranchesSuccess",
            "getBranchesError",
            "changeOpenBranchStatistic",
            "changeSelectedBranchTextUnitIds",
        );
    }
}

export default alt.createActions(DashboardPageActions);