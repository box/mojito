class ScreenshotViewerStore {

    constructor() {
        this.setDefaultState();
    }

    setDefaultState() {
        this.show = false;
        this.src = null;
        this.number = 0;
        this.total = 0;
        this.branchStatisticScreenshots = null;
        this.textUnits = [];
    }

    open() {
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
}

export default ScreenshotViewerStore;