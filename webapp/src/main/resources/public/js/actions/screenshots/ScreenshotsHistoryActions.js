import alt from "../../alt.js";

class ScreenshotsHistoryActions {

    constructor() {
        this.generateActions(
            "enableHistoryUpdate",
            "disableHistoryUpdate",
        );
    }
}

export default alt.createActions(ScreenshotsHistoryActions);
