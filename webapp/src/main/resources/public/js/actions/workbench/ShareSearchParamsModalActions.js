import alt from "../../alt";

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
