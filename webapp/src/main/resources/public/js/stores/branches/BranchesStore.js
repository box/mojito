import alt from "../../alt";
import BranchesDataSource from "../../actions/branches/BranchesDataSource";
import BranchesSearchParamStore from "./BranchesSearchParamStore";
import BranchesPageActions from "../../actions/branches/BranchesPageActions";
import BranchStatisticsContent from "../../sdk/entity/BranchStatisticsContent";
import BranchesScreenshotViewerActions from "../../actions/branches/BranchesScreenshotViewerActions";
import GitBlameScreenshotViewerStore from "../workbench/GitBlameScreenshotViewerStore";

class BranchesStore {
    constructor() {
        this.setDefaultState();
        this.bindActions(BranchesPageActions);
        this.bindActions(BranchesScreenshotViewerActions);

        this.registerAsync(BranchesDataSource);
    }

    static getBranchStatisticById(branchStatisticId) {
        const state = this.getState();
        const branchStatistics = state.branchStatistics.filter((b) => b.id === branchStatisticId);
        return branchStatistics.length > 0 ? branchStatistics[0] : null;
    }

    static getSelectedBranchStatistic() {
        const state = this.getState();
        return this.getBranchStatisticById(state.openBranchStatisticId);
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
            const textUnitsWithScreenshots = new Set();
            this.textUnitsWithScreenshotsByBranchStatisticId[this.branchStatistics[i].id] = textUnitsWithScreenshots;

            for (let j = 0; j < this.branchStatistics[i].branch.screenshots.length; j++) {
                for (let k = 0; k < this.branchStatistics[i].branch.screenshots[j].textUnits.length; k++) {
                    const tmTextUnitId = this.branchStatistics[i].branch.screenshots[j].textUnits[k].tmTextUnit.id;
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

    onDeleteScreenshotSuccess() {
        this.branchStatistics.find(branch => branch.id === this.openBranchStatisticId).branch.screenshots = GitBlameScreenshotViewerStore.state.branchStatisticScreenshots;
        this.computeTextUnitsWithScreenshotsByBranchStatisticId();
    }
}

export default alt.createStore(BranchesStore, "BranchesStore");