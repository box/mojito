import alt from "../../alt";

class GitBlameScreenshotViewerActions {

    constructor() {
        this.generateActions(
            "open",
            "close",
            "goToPrevious",
            "goToNext"
        );
    }
}

export default alt.createActions(GitBlameScreenshotViewerActions);