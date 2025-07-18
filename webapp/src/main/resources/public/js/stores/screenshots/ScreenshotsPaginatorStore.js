import alt from "../../alt.js";
import ScreenshotsPaginatorActions from "../../actions/screenshots/ScreenshotsPaginatorActions.js";
import ScreenshotsPageActions from "../../actions/screenshots/ScreenshotsPageActions.js";
import PaginatorStore from "../PaginatorStore.js";

class ScreenshotsPaginatorStore extends PaginatorStore {

    constructor() {
        super();
        this.bindActions(ScreenshotsPaginatorActions);
        this.bindActions(ScreenshotsPageActions);
    }

    setDefaultState() {
        super.setDefaultState();
        this.limit = 3;
    }

    resetScreenshotSearchParams() {
        super.resetSearchParams();
    }

    screenshotsSearchResultsReceivedSuccess(result) {
        super.searchResultsReceivedSuccess(result);
    }

    screenshotsSearchResultsReceivedError() {
        super.searchResultsReceivedError();
    }
}

export default alt.createStore(ScreenshotsPaginatorStore, 'ScreenshotsPaginatorStore');
