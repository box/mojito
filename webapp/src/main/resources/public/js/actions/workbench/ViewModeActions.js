import alt from "../../alt";

class ViewModeActions {

    constructor() {
        this.generateActions(
            "changeViewMode",
        );
    }
}

export default alt.createActions(ViewModeActions);
