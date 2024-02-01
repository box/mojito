import UserClient from "../../sdk/UserClient";
import UserActions from "./UserActions";

const UserDataSource = {
    getAllUsers: {
        remote(userStoreState, pageRequestParams) {
            return UserClient.getUsers(pageRequestParams.page, pageRequestParams.size);
        },

        success: UserActions.getAllUsersSuccess,
        error: UserActions.getAllUsersError
    },

    checkUsernameTakenRequest: {
        remote(userStoreState , username) {
            return UserClient.checkUsernameTaken(username);
        },

        success: UserActions.checkUsernameTakenSuccess,
        error: UserActions.checkUsernameTakenError
    },

    deleteRequest: {
        remote(userStoreState , id) {
            return UserClient.deleteUser(id);
        },

        success: UserActions.deleteRequestSuccess,
        error: UserActions.deleteRequestError
    },

    saveNewRequest: {
        remote(userStoreState, user) {
            return UserClient.saveNewUser(user);
        },

        success: UserActions.saveNewRequestSuccess,
        error: UserActions.saveNewRequestError
    },

    saveEditRequest: {
        remote(userStoreState, userParams) {
            return UserClient.saveEditUser(userParams);
        },

        success: UserActions.saveEditRequestSuccess,
        error: UserActions.saveEditRequestError
    },
};

export default UserDataSource;
