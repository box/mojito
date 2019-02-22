import alt from "../../alt";
import BranchesDataSource from "../../actions/branches/BranchesDataSource";
import BranchesSearchParamStore from "./BranchesSearchParamStore";
import BranchesPageActions from "../../actions/branches/BranchesPageActions";
import BranchStatisticsContent from "../../sdk/entity/BranchStatisticsContent";

class BranchesStore {
    constructor() {
        this.setDefaultState();
        this.bindActions(BranchesPageActions);
        this.registerAsync(BranchesDataSource);
    }

    setDefaultState() {
        this.branchStatistics = [];
        this.searching = false;
        this.openBranchStatisticId = null;
        this.selectedBranchTextUnitIds = [];
        this.textUnitsWithScreenshotsByBranchStatisticId = {};
    }

    getBranches() {
        this.waitFor(BranchesSearchParamStore);
        this.getInstance().performBranchesSearch();
        this.isSearching = true;
    }

    getBranchesSuccess(branchStatistics) {
        this.branchStatistics = BranchStatisticsContent.toContentList(branchStatistics.content);
        this.selectedBranchTextUnitIds = [];
        this.computeTextUnitsWithScreenshotsByBranchStatisticId();
        this.openIfSingleMatch();
        this.isSearching = false;
    }

    openIfSingleMatch() {
        if (this.branchStatistics.length === 1) {
            this.openBranchStatisticId = this.branchStatistics[0].id;
        } else {
            this.openBranchStatisticId = null;
        }
    }

    computeTextUnitsWithScreenshotsByBranchStatisticId() {
        for (let i = 0; i < this.branchStatistics.length; i++) {
            let textUnitsWithScreenshots = new Set();
            this.textUnitsWithScreenshotsByBranchStatisticId[this.branchStatistics[i].id] = textUnitsWithScreenshots;

            for (let j = 0; j < this.branchStatistics[i].branch.screenshots.length; j++) {
                for (let k = 0; k < this.branchStatistics[i].branch.screenshots[j].textUnits.length; k++) {
                    let tmTextUnitId = this.branchStatistics[i].branch.screenshots[j].textUnits[k].tmTextUnit.id
                    textUnitsWithScreenshots.add(tmTextUnitId);
                }
            }
        }
    }

    changeOpenBranchStatistic(branchStatisticId) {
        this.openBranchStatisticId = branchStatisticId;
    }

    changeSelectedBranchTextUnitIds(selectedBranchTextUnitIds) {
        this.selectedBranchTextUnitIds = selectedBranchTextUnitIds;
    }

    resetBranchesSearchParams() {
        this.setDefaultState();
    }

    static getBranchStatisticById(branchStatisticId) {
        let state = this.getState();
        let branchStatistics = state.branchStatistics.filter((b) => b.id === branchStatisticId);
        return branchStatistics.length > 0 ? branchStatistics[0] : null;
    }

    static getSelectedBranchStatistic() {
        let state = this.getState();
        return this.getBranchStatisticById(state.openBranchStatisticId);
    }
}

export default alt.createStore(BranchesStore, "BranchesStore")