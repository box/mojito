import alt from "../../alt";

class DashboardPageActions {

    constructor() {
        this.generateActions(
            "updateSearchParams",
            "getBranches",
            "getBranchesSuccess",
            "getBranchesError",
            "changeOpenBranchStatistic",
            "changeSelectedBranchTextUnitIds",
        );
    }
}

export default alt.createActions(DashboardPageActions);