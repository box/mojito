import alt from "../../alt.js";

class GitBlameScreenshotViewerActions {

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

export default alt.createActions(GitBlameScreenshotViewerActions);