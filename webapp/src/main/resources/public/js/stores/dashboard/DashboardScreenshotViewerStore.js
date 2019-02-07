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
    }

    open(branchIndex) {
        this.loadScreenshotByIndex(branchIndex, 1);
        this.show = true;
    }

    loadScreenshotByIndex(branchIndex, number) {

        let dashboardStoreState = DashboardStore.getState();

        let branchStatisticScreenshots = dashboardStoreState.branchStatistics[branchIndex].branch.screenshots

        console.log(branchStatisticScreenshots);

        if (branchStatisticScreenshots.length > 0) {
            this.number = number;
            this.branchIndex = branchIndex;
            this.src = branchStatisticScreenshots[this.number - 1].src;
            this.total = branchStatisticScreenshots.length;
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
