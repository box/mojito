import alt from "../../alt";

class GitBlameScreenshotViewerActions {

    constructor() {
        this.generateActions(
            "open",
            "close",
            "goToPrevious",
            "goToNext",
            "deleteFromWorkbench",
            "onDeleteSuccess",
            "onDeleteFailure"
        );
    }
}

export default alt.createActions(GitBlameScreenshotViewerActions);