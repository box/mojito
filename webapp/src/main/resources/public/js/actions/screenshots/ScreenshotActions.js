import alt from "../../alt";

class ScreenshotActions {

    constructor() {
        this.generateActions(
            "changeStatus",
            "changeStatusSuccess",
            "changeStatusError"
        );
    }
}

export default alt.createActions(ScreenshotActions);
