import $ from "jquery";
import alt from "../../alt";

class ScreenshotsSearchTextActions {

    constructor() {
        this.generateActions(
                "changeSearchAttribute",
                "changeSearchType",
                "changeSearchText",
                "changeStatus"
                );
    }
}

export default alt.createActions(ScreenshotsSearchTextActions);
