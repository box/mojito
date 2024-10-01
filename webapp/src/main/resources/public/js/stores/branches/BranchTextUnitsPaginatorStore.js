import PaginatorStore from "../PaginatorStore";
import alt from "../../alt";
import BranchTextUnitsPaginatorActions from "../../actions/branches/BranchTextUnitsPaginatorActions";
import BranchTextUnitsPageActions from "../../actions/branches/BranchTextUnitsPageActions";
import BranchesPageActions from "../../actions/branches/BranchesPageActions";
import BranchStatisticsContent from "../../sdk/entity/BranchStatisticsContent";

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
}

export default alt.createStore(BranchTextUnitsPaginatorStore, 'BranchTextUnitsPaginatorStore');
