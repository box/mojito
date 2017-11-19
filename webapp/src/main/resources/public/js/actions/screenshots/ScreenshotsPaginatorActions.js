import $ from "jquery";
import alt from "../../alt";

class ScreenshotsPaginatorActions {

    constructor() {
        this.generateActions(
            "goToNextPage",
            "goToPreviousPage",
            "changeCurrentPageNumber"
        );
    }
}

export default alt.createActions(ScreenshotsPaginatorActions);
