import alt from "../../alt";

class LocaleActions {
    constructor() {
        this.generateActions(
            "loadLocales",
            "loadLocalesSuccess",
            "loadLocalesError",
        );
    }
}

export default alt.createActions(LocaleActions);
