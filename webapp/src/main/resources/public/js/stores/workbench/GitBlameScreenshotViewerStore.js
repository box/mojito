import alt from "../../alt.js";
import GitBlameScreenshotViewerActions from "../../actions/workbench/GitBlameScreenshotViewerActions.js";
import ScreenshotViewerStore from "../ScreenshotViewerStore.js";
import ScreenshotViewerDataSource from "../../actions/screenshots/ScreenshotViewerDataSource.js";

class GitBlameScreenshotViewerStore extends ScreenshotViewerStore {

    constructor() {
        super();
        this.bindActions(GitBlameScreenshotViewerActions);

        this.registerAsync(new ScreenshotViewerDataSource(GitBlameScreenshotViewerActions));
    }
}

export default alt.createStore(GitBlameScreenshotViewerStore, 'GitBlameScreenshotViewerStore');
