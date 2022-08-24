import alt from "../../alt";

class ScreenshotViewerActions {

    constructor() {
        this.generateActions(
            "delete",
            "onDeleteSuccess",
            "onDeleteFailure"
        );
    }
}

export default alt.createActions(ScreenshotViewerActions);