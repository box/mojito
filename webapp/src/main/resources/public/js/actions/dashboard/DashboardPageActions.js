import alt from "../../alt";

class DashboardPageActions {

    constructor() {
        this.generateActions(





            "updateSearchParams",


            "getBranches",
            "getBranchesSuccess",
            "getBranchesError",
            "changeOpenBranchStatistic",

            "textUnitCheckboxChanged",
            "onBranchCollapseChange",
            "resetAllSelectedTextUnitsInCurrentPage"
        );
    }
}

export default alt.createActions(DashboardPageActions);