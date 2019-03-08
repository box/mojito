import alt from "../../alt";
import GitBlameScreenshotViewerActions from "../../actions/workbench/GitBlameScreenshotViewerActions";
import ScreenshotViewerStore from "../ScreenshotViewerStore";

class GitBlameScreenshotViewerStore extends ScreenshotViewerStore {

    constructor() {
        super();
        this.bindActions(GitBlameScreenshotViewerActions);
    }

    open(branchScreenshots) {
        this.branchStatisticScreenshots = branchScreenshots;
        super.open()
    }

}

export default alt.createStore(GitBlameScreenshotViewerStore, 'GitBlameScreenshotViewerStore');
