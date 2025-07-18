import alt from "../../alt.js";

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
