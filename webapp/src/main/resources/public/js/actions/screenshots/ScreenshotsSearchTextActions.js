import alt from "../../alt";

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
