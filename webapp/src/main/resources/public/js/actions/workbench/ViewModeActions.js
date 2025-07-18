import alt from "../../alt.js";

class ViewModeActions {

    constructor() {
        this.generateActions(
            "changeViewMode",
        );
    }
}

export default alt.createActions(ViewModeActions);
