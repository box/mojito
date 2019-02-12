import alt from "../../alt";

class DashboardPageActions {

    constructor() {
        this.generateActions(
            "updateSearchParams",
            "getBranches",
            "getBranchesSuccess",
            "getBranchesError",
            "textUnitCheckboxChanged",
            "onBranchCollapseChange",
            "resetAllSelectedTextUnitsInCurrentPage"
        );
    }
}

export default alt.createActions(DashboardPageActions);