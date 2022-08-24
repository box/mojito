import alt from "../../alt";
import GitBlameScreenshotViewerActions from "../../actions/workbench/GitBlameScreenshotViewerActions";
import ScreenshotViewerStore from "../ScreenshotViewerStore";
import ScreenshotViewerActions from "../../actions/screenshots/ScreenshotViewerActions";

class GitBlameScreenshotViewerStore extends ScreenshotViewerStore {

    constructor() {
        super();
        this.bindActions(GitBlameScreenshotViewerActions);
        this.bindActions(ScreenshotViewerActions);
    }

}

export default alt.createStore(GitBlameScreenshotViewerStore, 'GitBlameScreenshotViewerStore');
