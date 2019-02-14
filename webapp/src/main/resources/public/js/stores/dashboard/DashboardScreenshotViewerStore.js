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
        this.branchStatisticId = null;
        this.textUnits = [];
    }

    open(branchStatisticId) {
        this.loadScreenshotByIndex(branchStatisticId, 1);
        this.show = true;
    }

    loadScreenshotByIndex(branchStatisticId, number) {
        let dashboardStoreState = DashboardStore.getState();
        let branchStatisticScreenshots = DashboardStore.getBranchStatisticById(branchStatisticId).branch.screenshots

        if (branchStatisticScreenshots.length > 0) {
            this.number = number;
            this.branchStatisticId = branchStatisticId;
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
            this.loadScreenshotByIndex(this.branchStatisticId, this.number - 1);
        }
    }

    goToNext() {
        if (this.number < this.total) {
            this.loadScreenshotByIndex(this.branchStatisticId, this.number + 1);
        }
    }
}

export default alt.createStore(DashboardScreenshotViewerStore, 'DashboardScreenshotViewerStore');
