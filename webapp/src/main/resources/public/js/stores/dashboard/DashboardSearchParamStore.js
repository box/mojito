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
        this.searchText = "";
        this.isSpinnerShown = false;
        this.isMine = true;
        this.deleted = true;
        this.undeleted = true;
    }

    resetDashboardSearchParams() {
        this.setDefaultState();
    }

    changeSearchText(text) {
        this.searchText = text;
    }

    changeSearchFilter(filter) {
        this[filter] = !this[filter];
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