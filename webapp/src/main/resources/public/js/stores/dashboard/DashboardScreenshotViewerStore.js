import alt from "../../alt";
import DashboardScreenshotViewerActions from "../../actions/dashboard/DashboardScreenshotViewerActions";
import DashboardStore from "./DashboardStore";


class DashboardScreenshotViewerStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(DashboardScreenshotViewerActions);
    }

    setDefaultState() {
        this.show = false;
        this.src = null;
        this.number = 0;
        this.total = 0;
        this.branchIndex = null;
        this.textUnits = [];
    }

    open(branchIndex) {
        this.loadScreenshotByIndex(branchIndex, 1);
        this.show = true;
    }

    loadScreenshotByIndex(branchIndex, number) {

        let dashboardStoreState = DashboardStore.getState();

        let branchStatisticScreenshots = dashboardStoreState.branchStatistics[branchIndex].branch.screenshots

        if (branchStatisticScreenshots.length > 0) {
            this.number = number;
            this.branchIndex = branchIndex;
            let branchStatisticScreenshot = branchStatisticScreenshots[this.number - 1];
            this.src = branchStatisticScreenshot.src;
            this.total = branchStatisticScreenshots.length;
            this.textUnits = branchStatisticScreenshot.textUnits;
        }
    }

    close() {
        this.setDefaultState();
    }

    goToPrevious() {
        if (this.number > 1) {
            this.loadScreenshotByIndex(this.branchIndex, this.number - 1);
        }
    }

    goToNext() {
        if (this.number < this.total) {
            this.loadScreenshotByIndex(this.branchIndex, this.number + 1);
        }
    }
}

export default alt.createStore(DashboardScreenshotViewerStore, 'DashboardScreenshotViewerStore');
