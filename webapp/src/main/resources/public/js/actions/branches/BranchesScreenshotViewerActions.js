import alt from "../../alt";

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