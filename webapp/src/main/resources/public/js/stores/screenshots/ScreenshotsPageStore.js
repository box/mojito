import alt from "../../alt.js";
import ScreenshotsPageDataSource from "../../actions/screenshots/ScreenshotsPageDataSource.js";
import ScreenshotsPageActions from "../../actions/screenshots/ScreenshotsPageActions.js";
import ScreenshotActions from "../../actions/screenshots/ScreenshotActions.js";

class ScreenshotsPageStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(ScreenshotsPageActions);
        this.bindActions(ScreenshotActions);

        this.registerAsync(ScreenshotsPageDataSource);
    }

    setDefaultState() {
        this.selectedScreenshotIdx = 0;
        this.screenshotsData = [];
        this.searching = false;
    }

    resetScreenshotSearchParams() {
        this.setDefaultState();
    }

    performSearch() {
        this.getInstance().performScreenshotSearch();
        this.searching = true;
    }

    changeSelectedScreenshotIdx(selectedScreenshotIdx) {
        this.selectedScreenshotIdx = selectedScreenshotIdx;
    }

    screenshotsSearchResultsReceivedSuccess(result) {
        this.screenshotsData = result.content;
        if (this.selectedScreenshotIdx >= result.size) {
            this.selectedScreenshotIdx = 0;
        }
        this.searching = false;
    }

    changeStatusSuccess(res) {
        if (this.screenshotsData[res.idx]) {
            this.screenshotsData[res.idx].status = res.status;
            this.screenshotsData[res.idx].comment = res.comment;
        }
    }

    static getScreenshotByIdx(screenshotIdx) {

        let screenshot = null;
        const state = this.getState();

        if(!(screenshotIdx >= state.screenshotsData.length)) {
           screenshot = state.screenshotsData[screenshotIdx];
        }

        return screenshot;
    }
}

export default alt.createStore(ScreenshotsPageStore, 'ScreenshotsPageStore');
