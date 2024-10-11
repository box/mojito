import UserClient from "../../sdk/UserClient";
import UserActions from "./UserActions";
import UserModalActions from "./UserModalActions";
import UserSearcherParameters from "../../sdk/UserSearcherParameters";
import UserSearchParamStore from "../../stores/users/UserSearchParamStore";

const UserDataSource = {
    getUsers: {
        remote(userStoreState, pageRequestParams) {
            const userSearcherParameters = new UserSearcherParameters();
            const { searchText } = UserSearchParamStore.getState();
            const { page, size } = pageRequestParams;
            userSearcherParameters.search(searchText).page(page).size(size);
            return UserClient.getUsers(userSearcherParameters);
        },
        success: UserActions.getUsersSuccess,
        error: UserActions.getUsersError
    },

    checkUsernameTakenRequest: {
        remote(userStoreState , username) {
            return UserClient.checkUsernameTaken(username);
        },

        success: UserModalActions.checkUsernameTakenSuccess,
        error: UserModalActions.checkUsernameTakenError
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
