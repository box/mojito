import alt from "../../alt";

class ScreenshotsReviewModalActions {

    constructor() {
        this.generateActions( 
            "openWithScreenshot",
            "changeComment",
            "changeStatus",
            "save",
            "close"
        );
    }
}

export default alt.createActions(ScreenshotsReviewModalActions);
