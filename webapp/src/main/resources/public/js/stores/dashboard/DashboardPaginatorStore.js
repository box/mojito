import alt from "../../alt";
import DashboardPaginatorActions from "../../actions/dashboard/DashboardPaginatorActions";
import DashboardPageActions from "../../actions/dashboard/DashboardPageActions";
import PaginatorStore from "../PaginatorStore";

class DashboardPaginatorStore extends PaginatorStore {

    constructor() {
        super();
        this.bindActions(DashboardPaginatorActions);
        this.bindActions(DashboardPageActions);
    }

    resetDashboardSearchParams() {
        super.resetSearchParams();
    }

    getBranches() {
        super.performSearch();
    }

    getBranchesSuccess(result) {
        super.searchResultsReceivedSuccess(result);
    }

    getBranchesError() {
        super.searchResultsReceivedError();
    }
}

export default alt.createStore(DashboardPaginatorStore, 'DashboardPaginatorStore');
