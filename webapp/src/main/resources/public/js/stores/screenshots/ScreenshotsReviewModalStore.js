import alt from "../../alt";
import ScreenshotsReviewModalActions from "../../actions/screenshots/ScreenshotsReviewModalActions";
import ScreenshotsPageActions from "../../actions/screenshots/ScreenshotsPageActions";
import ScreenshotDataSource from "../../actions/screenshots/ScreenshotDataSource";
import ScreenshotsPageStore from "../../stores/screenshots/ScreenshotsPageStore";
import {StatusCommonTypes} from "../../components/screenshots/StatusCommon";

class ScreenshotsReviewModalStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(ScreenshotsReviewModalActions);
        this.bindActions(ScreenshotsPageActions);
        this.registerAsync(ScreenshotDataSource);
    }
    
    setDefaultState() {
        this.show = false;
        this.comment = "";
        this.status = StatusCommonTypes.ACCEPTED;
        this.saveDisabled = true;
        this.screenshotIdx = null;
        this.screenshot = null;
    }
    
    resetScreenshotSearchParams() {
        this.setDefaultState();
    }
    
    openWithScreenshot(screenshotIdx) {
        this.show = true;
        this.screenshotIdx = screenshotIdx;
        this.screenshot = ScreenshotsPageStore.getScreenshotByIdx(screenshotIdx);
        
        if (this.screenshot !== null) {
            this.comment = this.getScreenshotComment();
            this.status = this.getScreenshotStatus();
            this.saveDisabled = this.isSaveDisabled();
        } else {
            console.log("Show modal shouldn't be called, no screenshot selected");
        }
    }

    close() {
        this.show = false;
    }
    
    save() {
        this.show = false;
        this.getInstance().changeStatus(this.status, this.comment, this.screenshotIdx);
    }

    changeStatus(status) {
        this.status = status;
        this.saveDisabled = this.isSaveDisabled();
    }

    changeComment(comment) {
        this.comment = comment;
        this.saveDisabled = this.isSaveDisabled();
    }

    getScreenshotComment() {
        return this.screenshot.comment ? this.screenshot.comment : "";
    }
    getScreenshotStatus() {
        return this.screenshot.status ? this.screenshot.status : "";
    }

    isSaveDisabled(screenshot) {
        return this.screenshot && this.getScreenshotComment(screenshot) === this.comment &&
                this.getScreenshotStatus(screenshot) === this.status;
    }
}

export default alt.createStore(ScreenshotsReviewModalStore, 'ScreenshotsReviewModalStore');
