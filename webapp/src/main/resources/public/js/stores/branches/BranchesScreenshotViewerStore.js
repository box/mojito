import alt from "../../alt.js";
import BranchesScreenshotViewerActions from "../../actions/branches/BranchesScreenshotViewerActions.js";
import BranchesStore from "./BranchesStore.js";
import ScreenshotViewerStore from "../ScreenshotViewerStore.js";
import ScreenshotViewerDataSource from "../../actions/screenshots/ScreenshotViewerDataSource.js";

class BranchesScreenshotViewerStore extends ScreenshotViewerStore {

    constructor() {
        super();
        this.bindActions(BranchesScreenshotViewerActions);

        this.registerAsync(new ScreenshotViewerDataSource(BranchesScreenshotViewerActions));
    }

    openScreenshotsViewer(branchStatisticId) {
        const branchStatisticScreenshots = BranchesStore.getBranchStatisticById(branchStatisticId).branch.screenshots;
        super.openScreenshotsViewer(branchStatisticScreenshots);
    }
}

export default alt.createStore(BranchesScreenshotViewerStore, 'BranchesScreenshotViewerStore');
