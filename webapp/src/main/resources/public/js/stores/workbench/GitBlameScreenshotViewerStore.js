import alt from "../../alt";
import GitBlameScreenshotViewerActions from "../../actions/workbench/GitBlameScreenshotViewerActions";

class GitBlameScreenshotViewerStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(GitBlameScreenshotViewerActions);
    }

    setDefaultState() {
        this.show = false;
        this.src = null;
        this.number = 0;
        this.total = 0;
        this.branchScreenshots = null;
        this.textUnits = [];
    }

    open(branchScreenshots) {
        this.loadScreenshot(branchScreenshots, 1);
        this.show = true;
    }

    loadScreenshot(branchScreenshots, number) {
        if (branchScreenshots.length > 0) {
            this.number = number;
            let branchStatisticScreenshot = branchScreenshots[this.number - 1];
            this.branchScreenshots = branchScreenshots;
            this.src = branchStatisticScreenshot.src;
            this.total = branchScreenshots.length;
            this.textUnits = branchStatisticScreenshot.textUnits;
        }
    }

    close() {
        this.setDefaultState();
    }

    goToPrevious() {
        if (this.number > 1) {
            this.loadScreenshot(this.branchScreenshots, this.number - 1);
        }
    }

    goToNext() {
        if (this.number < this.total) {
            this.loadScreenshot(this.branchScreenshots, this.number + 1);
        }
    }
}

export default alt.createStore(GitBlameScreenshotViewerStore, 'GitBlameScreenshotViewerStore');
