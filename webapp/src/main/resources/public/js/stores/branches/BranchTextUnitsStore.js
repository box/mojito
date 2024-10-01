import alt from "../../alt";
import BranchTextUnitsDataSource from "../../actions/branches/BranchTextUnitsDataSource";
import BranchTextUnitsPageActions from "../../actions/branches/BranchTextUnitsPageActions";
import BranchTextUnitsParamStore from "./BranchTextUnitsParamStore";
import BranchTextUnitStatistics from "../../sdk/entity/BranchTextUnitStatistics";
import BranchesPageActions from "../../actions/branches/BranchesPageActions";
import BranchStatisticsContent from "../../sdk/entity/BranchStatisticsContent";

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
