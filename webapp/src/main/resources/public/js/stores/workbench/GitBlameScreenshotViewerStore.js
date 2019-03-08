import alt from "../../alt";
import GitBlameScreenshotViewerActions from "../../actions/workbench/GitBlameScreenshotViewerActions";
import ScreenshotViewerStore from "../ScreenshotViewerStore";

class GitBlameScreenshotViewerStore extends ScreenshotViewerStore {

    constructor() {
        super();
        this.bindActions(GitBlameScreenshotViewerActions);
    }

}

export default alt.createStore(GitBlameScreenshotViewerStore, 'GitBlameScreenshotViewerStore');
