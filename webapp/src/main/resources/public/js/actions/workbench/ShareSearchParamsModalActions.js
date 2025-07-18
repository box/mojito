import alt from "../../alt.js";

class ShareSearchParamsModalActions {

    constructor() {
        this.generateActions(
            "open",
            "close",

            "saveSearchParamsSuccess",
            "saveSearchParamsError",

            "getSearchParams",
            "getSearchParamsSuccess",
            "getSearchParamsError",

            "setErrorType"
        );
    }
}

export default alt.createActions(ShareSearchParamsModalActions);
