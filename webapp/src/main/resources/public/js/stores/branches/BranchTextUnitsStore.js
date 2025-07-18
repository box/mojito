import alt from "../../alt.js";
import BranchTextUnitsDataSource from "../../actions/branches/BranchTextUnitsDataSource.js";
import BranchTextUnitsPageActions from "../../actions/branches/BranchTextUnitsPageActions.js";
import BranchTextUnitsParamStore from "./BranchTextUnitsParamStore.js";
import BranchTextUnitStatistics from "../../sdk/entity/BranchTextUnitStatistics.js";
import BranchesPageActions from "../../actions/branches/BranchesPageActions.js";
import BranchStatisticsContent from "../../sdk/entity/BranchStatisticsContent.js";

class BranchTextUnitsStore {
    constructor() {
        this.setDefaultState();

        this.bindActions(BranchesPageActions);
        this.bindActions(BranchTextUnitsPageActions);
        this.registerAsync(BranchTextUnitsDataSource);
    }

    setDefaultState() {
        this.branchTextUnitStatistics = [];
    }

    getBranchTextUnits() {
        this.waitFor(BranchTextUnitsParamStore);
        this.getInstance().performBranchTextUnitsSearch();
    }

    resetBranchTextUnitsSearchParams() {
        this.setDefaultState();
    }

    getBranchTextUnitsSuccess(branchTextUnitStatistics) {
        this.branchTextUnitStatistics =
            BranchTextUnitStatistics.toBranchTextUnitStatisticsList(branchTextUnitStatistics.content);
    }

    getBranchesSuccess(result) {
        const branchStatistics = BranchStatisticsContent.toContentList(result.content);
        if (branchStatistics.length === 1) {
            const { isPaginated } = branchStatistics[0];
            if (isPaginated) {
                this.getBranchTextUnits();
            }
        }
    }
}

export default alt.createStore(BranchTextUnitsStore, "BranchTextUnitsStore");
