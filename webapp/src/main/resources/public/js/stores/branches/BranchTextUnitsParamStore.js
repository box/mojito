import alt from "../../alt";
import BranchTextUnitsPageActions from "../../actions/branches/BranchTextUnitsPageActions";
import BranchTextUnitsParamActions from "../../actions/branches/BranchTextUnitsParamActions";
import BranchesPageActions from "../../actions/branches/BranchesPageActions";
import BranchStatisticsContent from "../../sdk/entity/BranchStatisticsContent";

class BranchTextUnitsParamStore {
    constructor() {
        this.setDefaultState();
        this.bindActions(BranchesPageActions);
        this.bindActions(BranchTextUnitsParamActions);
        this.bindActions(BranchTextUnitsPageActions);
    }

    setDefaultState() {
        this.isSpinnerShown = false;
        this.branchStatisticId = null;
    }

    changeBranchStatisticId(branchStatisticId) {
        this.branchStatisticId = branchStatisticId;
    }

    getBranchTextUnits() {
        this.isSpinnerShown = true;
    }

    resetBranchTextUnitsSearchParams() {
        this.setDefaultState();
    }

    getBranchTextUnitsSuccess() {
        this.isSpinnerShown = false;
    }

    getBranchTextUnitsError() {
        this.isSpinnerShown = false;
    }

    getBranchesSuccess(result) {
        const branchStatistics = BranchStatisticsContent.toContentList(result.content);
        if (branchStatistics.length === 1) {
            this.branchStatisticId = branchStatistics[0].id;
        } else {
            this.branchStatisticId = null;
        }
    }
}

export default alt.createStore(BranchTextUnitsParamStore, 'BranchTextUnitsParamStore');
