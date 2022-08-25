import alt from "../../alt";

class BranchesScreenshotViewerActions {

    constructor() {
        this.generateActions(
            "open",
            "close",
            "goToPrevious",
            "goToNext",
            "delete",
            "onDeleteSuccess",
            "onDeleteFailure"
        );
    }
}

export default alt.createActions(BranchesScreenshotViewerActions);