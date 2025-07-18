import alt from "../../alt.js";
import ScreenshotDataSource from "../../actions/screenshots/ScreenshotDataSource.js";
import ScreenshotActions from "../../actions/screenshots/ScreenshotActions.js";

class ScreenshotStore {

    constructor() {
        this.bindActions(ScreenshotActions);
        this.registerAsync(ScreenshotDataSource);
    }

    changeStatus(data) {
        this.getInstance().changeStatus(data.status, data.comment, data.idx);
    }

    changeStatusSuccess() {}

}

export default alt.createStore(ScreenshotStore, 'ScreenshotStore');
