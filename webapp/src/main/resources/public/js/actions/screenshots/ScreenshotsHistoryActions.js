import alt from "../../alt";

class ScreenshotsHistoryActions {

    constructor() {
        this.generateActions(
            "enableHistoryUpdate",
            "disableHistoryUpdate",
        );
    }
}

export default alt.createActions(ScreenshotsHistoryActions);
