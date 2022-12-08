import alt from "../../alt";
import BranchesScreenshotViewerActions from "../../actions/branches/BranchesScreenshotViewerActions";
import BranchesStore from "./BranchesStore";
import ScreenshotViewerStore from "../ScreenshotViewerStore";
import ScreenshotsDataSource from "../../actions/screenshots/ScreenshotsDataSource";

class BranchesScreenshotViewerStore extends ScreenshotViewerStore {

    constructor() {
        super();
        this.bindActions(BranchesScreenshotViewerActions);
        this.registerAsync(new ScreenshotsDataSource(BranchesScreenshotViewerActions));
    }

    open(branchStatisticId) {
        const branchStatisticScreenshots = BranchesStore.getBranchStatisticById(branchStatisticId).branch.screenshots;
        super.open(branchStatisticScreenshots);
    }

    // TODO remove overload, this is just to demo and is not needed
    onDeleteSuccess() {
        console.log('BranchesScreenshotViewerStore::onDeleteSuccess')
        super.onDeleteSuccess();
    }

}

export default alt.createStore(BranchesScreenshotViewerStore, 'BranchesScreenshotViewerStore');
