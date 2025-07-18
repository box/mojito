import alt from "../../alt.js";

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
            "deleteRequest",
            "deleteRequestSuccess",
            "deleteRequestError",
            "saveNewRequest",
            "saveNewRequestSuccess",
            "saveNewRequestError",
            "saveEditRequest",
            "saveEditRequestSuccess",
            "saveEditRequestError",
            "getUsers",
            "getUsersSuccess",
            "getUsersError",
        );
    }
}

export default alt.createActions(UserActions);
