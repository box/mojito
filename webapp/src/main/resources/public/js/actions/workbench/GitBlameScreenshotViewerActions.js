import alt from "../../alt";

class GitBlameScreenshotViewerActions {

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

export default alt.createActions(GitBlameScreenshotViewerActions);