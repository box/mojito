import alt from "../../alt.js";
import BranchesPaginatorActions from "../../actions/branches/BranchesPaginatorActions.js";
import BranchesPageActions from "../../actions/branches/BranchesPageActions.js";
import PaginatorStore from "../PaginatorStore.js";

class BranchesPaginatorStore extends PaginatorStore {

    constructor() {
        super();
        this.bindActions(BranchesPaginatorActions);
        this.bindActions(BranchesPageActions);
    }

    resetBranchesSearchParams() {
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

export default alt.createStore(BranchesPaginatorStore, 'BranchesPaginatorStore');
