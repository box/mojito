import alt from "../../alt";

class DashboardScreenshotViewerActions {

    constructor() {
        this.generateActions(
            "open",
            "close",
            "goToPrevious",
            "goToNext"
        );
    }
}

export default alt.createActions(DashboardScreenshotViewerActions);