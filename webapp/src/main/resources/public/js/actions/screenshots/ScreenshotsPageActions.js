import alt from "../../alt.js";

class ScreenshotsPageActions {

    constructor() {
        this.generateActions(
            "changeSelectedScreenshotIdx",
            "performSearch",
            "screenshotsSearchResultsReceivedSuccess",
            "screenshotsSearchResultsReceivedError",
            "resetScreenshotSearchParams"
        );
    }
}

export default alt.createActions(ScreenshotsPageActions);
