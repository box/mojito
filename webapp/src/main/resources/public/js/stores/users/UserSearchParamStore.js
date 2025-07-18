import alt from "../../alt.js";
import UserSearchParamActions from "../../actions/users/UserSearchParamActions.js";
import UserActions from "../../actions/users/UserActions.js";

class UserSearchParamStore {
    constructor() {
        this.setDefaultState();

        this.bindActions(UserSearchParamActions);
        this.bindActions(UserActions);
    }

    setDefaultState() {
        this.searchText = "";
        this.isSpinnerShown = false;
    }

    changeSearchText(text) {
        this.searchText = text;
    }

    resetUserSearchParams() {
        this.setDefaultState();
    }

    getUsers() {
        this.isSpinnerShown = true;
    }

    getUsersSuccess() {
        this.isSpinnerShown = false;
    }

    getUsersError() {
        this.isSpinnerShown = false;
    }
}

export default alt.createStore(UserSearchParamStore, "UserSearchParamStore");
