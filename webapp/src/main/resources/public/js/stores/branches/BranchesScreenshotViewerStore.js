import alt from "../../alt";
import BranchesScreenshotViewerActions from "../../actions/branches/BranchesScreenshotViewerActions";
import BranchesStore from "./BranchesStore";
import ScreenshotViewerStore from "../ScreenshotViewerStore";
import ScreenshotViewerDataSource from "../../actions/screenshots/ScreenshotViewerDataSource";

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
