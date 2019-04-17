import alt from "../../alt";

class ScreenshotsLocaleActions {

    constructor() {
        this.generateActions(
            "changeSelectedBcp47Tags",
            "changeDropdownOpen"
        );
    }
}

export default alt.createActions(ScreenshotsLocaleActions);
