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
            "selectAllTextUnitsInCurrentPage",
            "resetAllSelectedTextUnitsInCurrentPage",
            "fetchPreviousPage",
            "fetchNextPage"
        );
    }
}

export default alt.createActions(DashboardPageActions);