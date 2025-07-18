import alt from "../../alt.js";

class ScreenshotsSearchTextActions {

    constructor() {
        this.generateActions(
                "changeSearchAttribute",
                "changeSearchType",
                "changeSearchText",
                "changeStatus",
                "changeScreenshotRunType"
                );
    }
}

export default alt.createActions(ScreenshotsSearchTextActions);
