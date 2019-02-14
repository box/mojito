import alt from "../../alt";

import DashboardSearchParamsActions from "../../actions/dashboard/DashboardSearchParamsActions";
import DashboardPageActions from "../../actions/dashboard/DashboardPageActions";


class DashboardSearchParamStore {
    constructor() {

        this.setDefaultState();
        this.bindActions(DashboardSearchParamsActions);
        this.bindActions(DashboardPageActions);
    }

    setDefaultState() {
        this.deleted = false;
        this.undeleted = true;
        this.onlyMyBranches = true;
        this.searchText = "";
        this.isSpinnerShown = false;
    }

    changeDeleted(deleted) {
        this.deleted = deleted;
    }

    changeUndeleted(undeleted) {
        this.undeleted = undeleted;
    }

    changeOnlyMyBranches(onlyMyBranches) {
        console.log("change branch", onlyMyBranches)
        this.onlyMyBranches = onlyMyBranches;
    }

    resetDashboardSearchParams() {
        this.setDefaultState();
    }

    changeSearchText(text) {
        this.searchText = text;
    }

    getBranches() {
        this.isSpinnerShown = true;
    }

    getBranchesSuccess() {
        this.isSpinnerShown = false;
    }

    getBranchesError() {
        this.isSpinnerShown = false;
    }

}

export default alt.createStore(DashboardSearchParamStore, 'DashboardSearchParamStore');