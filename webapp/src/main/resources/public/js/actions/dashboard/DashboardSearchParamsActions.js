import alt from "../../alt";

class DashboardSearchParamsActions {

    constructor() {
        this.generateActions(
            "changeDeleted",
            "changeUndeleted",
            "changeOnlyMyBranches",
            "changeSearchText",
            "resetDashboardSearchParams",
        );
    }
}

export default alt.createActions(DashboardSearchParamsActions);