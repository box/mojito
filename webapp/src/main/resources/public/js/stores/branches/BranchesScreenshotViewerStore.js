import alt from "../../alt";
import BranchesScreenshotViewerActions from "../../actions/branches/BranchesScreenshotViewerActions";
import BranchesStore from "./BranchesStore";
import ScreenshotViewerStore from "../ScreenshotViewerStore";


class BranchesScreenshotViewerStore extends ScreenshotViewerStore {

    constructor() {
        super();
        this.bindActions(BranchesScreenshotViewerActions);
    }

    open(branchStatisticId) {
        this.branchStatisticScreenshots = BranchesStore.getBranchStatisticById(branchStatisticId).branch.screenshots;
        super.open();
    }

}

export default alt.createStore(BranchesScreenshotViewerStore, 'BranchesScreenshotViewerStore');
