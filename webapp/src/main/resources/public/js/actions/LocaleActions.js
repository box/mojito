import alt from "../alt";

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
