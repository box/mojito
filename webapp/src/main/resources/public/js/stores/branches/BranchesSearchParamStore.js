import alt from "../../alt";

import BranchesSearchParamsActions from "../../actions/branches/BranchesSearchParamsActions";
import BranchesPageActions from "../../actions/branches/BranchesPageActions";


class BranchesSearchParamStore {
    constructor() {
        this.setDefaultState();
        this.bindActions(BranchesSearchParamsActions);
        this.bindActions(BranchesPageActions);
    }

    setDefaultState() {
        this.deleted = false;
        this.undeleted = true;
        this.empty = false;
        this.notEmpty = true;
        this.onlyMyBranches = true;
        this.searchText = "";
        this.isSpinnerShown = false;
        this.createdBefore = null;
        this.createdAfter = null;
    }

    changeCreatedBefore(createdBefore) {
        this.createdBefore = createdBefore;
    }

    changeCreatedAfter(createdAfter) {
        this.createdAfter = createdAfter;
    }

    changeDeleted(deleted) {
        this.deleted = deleted;
    }

    changeUndeleted(undeleted) {
        this.undeleted = undeleted;
    }

    changeEmpty(empty) {
        this.empty = empty;
    }

    changeNotEmpty(notEmpty) {
        this.notEmpty = notEmpty;
    }

    changeOnlyMyBranches(onlyMyBranches) {
        this.onlyMyBranches = onlyMyBranches;
    }

    resetBranchesSearchParams() {
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

export default alt.createStore(BranchesSearchParamStore, 'BranchesSearchParamStore');