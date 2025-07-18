import alt from "../../alt.js";

class UserSearchParamActions {
    constructor() {
        this.generateActions(
            "changeSearchText",
            "resetUserSearchParams",
        );
    }
}

export default alt.createActions(UserSearchParamActions);
