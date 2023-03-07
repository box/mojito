import alt from "../../alt";
import GitBlameScreenshotViewerActions from "../../actions/workbench/GitBlameScreenshotViewerActions";
import ScreenshotViewerStore from "../ScreenshotViewerStore";
import ScreenshotViewerDataSource from "../../actions/screenshots/ScreenshotViewerDataSource";

class GitBlameScreenshotViewerStore extends ScreenshotViewerStore {

    constructor() {
        super();
        this.bindActions(GitBlameScreenshotViewerActions);

        this.registerAsync(new ScreenshotViewerDataSource(GitBlameScreenshotViewerActions));
    }
}

export default alt.createStore(GitBlameScreenshotViewerStore, 'GitBlameScreenshotViewerStore');
