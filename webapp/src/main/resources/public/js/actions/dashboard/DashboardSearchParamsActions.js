import alt from "../../alt";

class DashboardSearchParamsActions {

    constructor() {
        this.generateActions(
            "changeSearchFilter",
            "changeSearchText",
            "resetDashboardSearchParams",

        );
    }
}

export default alt.createActions(DashboardSearchParamsActions);