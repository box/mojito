import alt from "../alt.js";

class LocaleActions {
    constructor() {
        this.generateActions(
            "getLocales",
            "getLocalesSuccess",
            "getLocalesError"
        );
    }
}

export default alt.createActions(LocaleActions);
