import alt from "../../alt";
import ViewModeActions from "../../actions/workbench/ViewModeActions";

class ViewModeStore {
    constructor() {
        this.viewMode = window.localStorage.getItem("viewMode");
        if (this.viewMode == null) {
            this.viewMode = ViewModeStore.VIEW_MODE.STANDARD;
            window.localStorage.setItem("viewMode", this.viewMode)
        }

        this.bindActions(ViewModeActions);
    }

    changeViewMode(viewMode) {
        this.viewMode = viewMode;
        window.localStorage.setItem("viewMode", this.viewMode)
    }
}

ViewModeStore.VIEW_MODE = {
    "FULL": "FULL",
    "REDUCED": "REDUCED",
    "STANDARD": "STANDARD",
    "COMPACT": "COMPACT",
}

export default alt.createStore(ViewModeStore, 'ViewModeStore');
