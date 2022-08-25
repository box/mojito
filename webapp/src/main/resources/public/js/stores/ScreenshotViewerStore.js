import ScreenshotsDataSource from "../actions/screenshots/ScreenshotsDataSource";

class ScreenshotViewerStore {

    constructor() {
        this.setDefaultState();
    }

    setDefaultState() {
        this.show = false;
        this.src = null;
        this.number = 0;
        this.total = 0;
        this.branchStatisticScreenshots = [];
        this.textUnits = [];
        this.isDeleting = false
    }

    open(branchStatisticsScreenshots) {
        this.branchStatisticScreenshots = branchStatisticsScreenshots;
        this.loadScreenshot(1);
        this.show = true;
    }

    loadScreenshot(number) {
        if (this.branchStatisticScreenshots.length > 0) {
            this.number = number;
            this.total = this.branchStatisticScreenshots.length;
            let branchStatisticScreenshot = this.branchStatisticScreenshots[this.number - 1];
            this.src = branchStatisticScreenshot.src;
            this.textUnits = branchStatisticScreenshot.textUnits;
        }
    }

    close() {
        this.setDefaultState();
    }

    goToPrevious() {
        if (this.number > 1) {
            this.loadScreenshot(this.number - 1);
        }
    }

    goToNext() {
        if (this.number < this.total) {
            this.loadScreenshot(this.number + 1);
        }
    }

    delete() {
        console.log("delete func is being accessed by both stores simultaneously")
        this.isDeleting = true;
        this.getInstance().delete()
    }

    onDeleteSuccess() {
        console.log("onDeleteSuccess func would also be accessed by both stores simultaneously on the DataSource callback")
        // if (this.branchStatisticScreenshots.length - 1) {
        //     this.branchStatisticScreenshots.splice(this.number - 1, 1)
        //     this.loadScreenshot(1);
        // } else {
        //     this.close()
        // }
        this.isDeleting = false
    }

    onDeleteFailure() {
        this.isDeleting = false
    }
}

export default ScreenshotViewerStore;