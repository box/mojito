import alt from "../../alt.js";

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
