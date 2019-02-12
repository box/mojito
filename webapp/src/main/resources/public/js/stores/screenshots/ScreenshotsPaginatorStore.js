import alt from "../../alt";
import ScreenshotsPaginatorActions from "../../actions/screenshots/ScreenshotsPaginatorActions";
import ScreenshotsPageActions from "../../actions/screenshots/ScreenshotsPageActions";
import Paginator from "../../components/screenshots/Paginator";
import PaginatorStore from "../PaginatorStore";

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
