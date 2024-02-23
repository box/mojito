import alt from "../../alt";

class UserActions {
    constructor() {
        this.generateActions(
            "closeUserModal",
            "openNewUserModal",
            "openEditUserModal",
            "openDeleteUserModal",
            "prevPage",
            "nextPage",
            "updatePageSize",
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
