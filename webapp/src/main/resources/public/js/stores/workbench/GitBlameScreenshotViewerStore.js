import alt from "../../alt";
import GitBlameScreenshotViewerActions from "../../actions/workbench/GitBlameScreenshotViewerActions";
import ScreenshotViewerStore from "../ScreenshotViewerStore";
import ScreenshotsDataSource from "../../actions/screenshots/ScreenshotsDataSource";

class GitBlameScreenshotViewerStore extends ScreenshotViewerStore {

    constructor() {
        super();
        this.bindActions(GitBlameScreenshotViewerActions);
        this.registerAsync(new ScreenshotsDataSource(GitBlameScreenshotViewerActions));
    }

    // TODO remove overload, this is just to demo and is not needed
    onDeleteSuccess() {
        console.log('GitBlameScreenshotViewerStore::onDeleteSuccess')
        super.onDeleteSuccess();
    }

}

export default alt.createStore(GitBlameScreenshotViewerStore, 'GitBlameScreenshotViewerStore');
