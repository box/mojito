import PaginatorStore from "../PaginatorStore.js";
import alt from "../../alt.js";
import BranchTextUnitsPaginatorActions from "../../actions/branches/BranchTextUnitsPaginatorActions.js";
import BranchTextUnitsPageActions from "../../actions/branches/BranchTextUnitsPageActions.js";
import BranchesPageActions from "../../actions/branches/BranchesPageActions.js";
import BranchStatisticsContent from "../../sdk/entity/BranchStatisticsContent.js";

class BranchTextUnitsPaginatorStore extends PaginatorStore {
    constructor() {
        super();
        this.bindActions(BranchesPageActions);
        this.bindActions(BranchTextUnitsPaginatorActions);
        this.bindActions(BranchTextUnitsPageActions);
    }

    getBranchTextUnits() {
        super.performSearch();
    }

    resetBranchTextUnitsSearchParams() {
        super.resetSearchParams();
    }

    getBranchTextUnitsSuccess(result) {
        super.searchResultsReceivedSuccess(result);
    }

    getBranchTextUnitsError() {
        super.searchResultsReceivedError();
    }

    getBranchesSuccess(result) {
        const branchStatistics = BranchStatisticsContent.toContentList(result.content);
        if (branchStatistics.length === 1) {
            this.resetBranchTextUnitsSearchParams();
        }
    }

    changePageSize(pageSize) {
        this.resetBranchTextUnitsSearchParams();
        this.limit = pageSize;
    }
}

export default alt.createStore(BranchTextUnitsPaginatorStore, 'BranchTextUnitsPaginatorStore');
