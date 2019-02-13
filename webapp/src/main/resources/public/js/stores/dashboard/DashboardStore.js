import alt from "../../alt";
import DashboardDataSource from "../../actions/dashboard/DashboardDataSource";
import DashboardSearchParamStore from "./DashboardSearchParamStore";
import DashboardPageActions from "../../actions/dashboard/DashboardPageActions";
import BranchStatisticsContent from "../../sdk/entity/BranchStatisticsContent";

class DashboardStore {
    constructor() {
        /**
         *
         * @type {branchStatistics[]}
         */
        this.setDefaultState();

        this.bindActions(DashboardPageActions);
        this.registerAsync(DashboardDataSource);
    }

    setDefaultState() {
        this.branchStatistics = [];
        this.searching = false;
        this.screenshotUploaded = {};

        this.openBranchIndex = -1;
        this.numberOfTextUnitChecked = 0;

        this.openBranchStatisticId = null;
        this.selectedBranchTextUnitIds = [];

        this.textUnitsWithScreenshotsByBranchStatisticId = {};
    }

    getBranches() {
        this.waitFor(DashboardSearchParamStore);
        this.getInstance().performDashboardSearch();
        this.isSearching = true;
    }

    getBranchesSuccess(branchStatistics) {
        this.branchStatistics = BranchStatisticsContent.toContentList(branchStatistics.content);
        this.isSearching = false;
        this.computeTextUnitsWithScreenshotsByBranchStatisticId();
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

export default alt.createStore(DashboardStore, "DashboardStore")