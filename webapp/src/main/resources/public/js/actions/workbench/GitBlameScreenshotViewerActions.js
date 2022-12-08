import alt from "../../alt";

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