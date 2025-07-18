import alt from "../../alt.js";

class BoxSDKConfigActions {

    constructor() {
        this.generateActions(
            "getConfig",
            "getConfigSuccess",
            "getConfigError",
            "setConfig",
            "setConfigSuccess",
            "setConfigError"
        );
    }
}

export default alt.createActions(BoxSDKConfigActions);
