import alt from "../../alt";

class BranchesScreenshotViewerActions {

    constructor() {
        this.generateActions(
            "open",
            "close",
            "goToPrevious",
            "goToNext"
        );
    }
}

export default alt.createActions(BranchesScreenshotViewerActions);