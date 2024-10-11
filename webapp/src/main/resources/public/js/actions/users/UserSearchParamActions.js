import alt from "../../alt";

class UserSearchParamActions {
    constructor() {
        this.generateActions(
            "changeSearchText",
            "resetUserSearchParams",
        );
    }
}

export default alt.createActions(UserSearchParamActions);
