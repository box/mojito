import alt from "../../alt.js";

class BranchesScreenshotViewerActions {

    constructor() {
        this.generateActions(
            "openScreenshotsViewer",
            "closeScreenshotsViewer",
            "goToPrevious",
            "goToNext",
            "delete",
            "onDeleteScreenshotSuccess",
            "onDeleteScreenshotFailure"
        );
    }
}

export default alt.createActions(BranchesScreenshotViewerActions);