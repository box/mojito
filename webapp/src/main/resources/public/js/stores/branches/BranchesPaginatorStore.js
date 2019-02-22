import alt from "../../alt";
import BranchesPaginatorActions from "../../actions/branches/BranchesPaginatorActions";
import BranchesPageActions from "../../actions/branches/BranchesPageActions";
import PaginatorStore from "../PaginatorStore";

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
