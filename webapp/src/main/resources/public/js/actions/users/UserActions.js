import alt from "../../alt";

class UserActions {
    constructor() {
        this.generateActions(
            "closeUserModal",
            "openNewUserModal",
            "openEditUserModal",
            "openDeleteUserModal",
            "reloadCurrentPage",
            "checkUsernameTaken",
            "checkUsernameTakenSuccess",
            "checkUsernameTakenError",
            "getAllUsers",
            "getAllUsersSuccess",
            "getAllUsersError",
            "deleteRequest",
            "deleteRequestSuccess",
            "deleteRequestError",
            "saveNewRequest",
            "saveNewRequestSuccess",
            "saveNewRequestError",
            "saveEditRequest",
            "saveEditRequestSuccess",
            "saveEditRequestError",
        );
    }
}

export default alt.createActions(UserActions);
