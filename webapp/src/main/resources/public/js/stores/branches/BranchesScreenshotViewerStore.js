import alt from "../../alt";
import BranchesScreenshotViewerActions from "../../actions/branches/BranchesScreenshotViewerActions";
import BranchesStore from "./BranchesStore";


class BranchesScreenshotViewerStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(BranchesScreenshotViewerActions);
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
        let branchStatisticScreenshots = BranchesStore.getBranchStatisticById(branchStatisticId).branch.screenshots

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

export default alt.createStore(BranchesScreenshotViewerStore, 'BranchesScreenshotViewerStore');
