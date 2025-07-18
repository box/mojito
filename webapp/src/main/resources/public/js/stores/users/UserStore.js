import alt from "../../alt.js";
import UserActions from "../../actions/users/UserActions.js";
import UserDataSource from "../../actions/users/UserDataSource.js";
import User from "../../sdk/entity/User.js";
import UserPage from "../../sdk/UsersPage.js";
import UserStatics from "../../utils/UserStatics.js";
import PageRequestParams from "../../sdk/PageRequestParams.js";
import UserSearchParamStore from "./UserSearchParamStore.js";

class UserStore {
    constructor() {
        /** @type {UserPage} */
        this.userPage = null;

        /** @type {User} */
        this.currentUser = new User();

        /** @type {String} */
        this.modalState = UserStatics.stateHidden();

        /** @type {Boolean} */
        this.showDeleteUserModal = false;

        /** @type {String} */
        this.lastErrorKey = null;

        /** @type {Number} */
        this.lastErrorCode = 0;

        this.isSearching = false;

        this.bindActions(UserActions);
        this.registerAsync(UserDataSource);
    }

    onCloseUserModal() {
        this.currentUser = new User();
        this.modalState = UserStatics.stateHidden();
        this.showDeleteUserModal = false;
        this.lastErrorKey = null;
    }

    onOpenNewUserModal() {
        this.currentUser = new User();
        this.modalState = UserStatics.stateNew();
    }

    /**
     * @param {User} user
     */
    onOpenEditUserModal(user) {
        this.currentUser = user;
        this.modalState = UserStatics.stateEdit();
    }

    onOpenDeleteUserModal(user) {
        this.currentUser = user;
        this.showDeleteUserModal = true;
    }


    prevPage() {
        const prevPage = this.userPage ? this.userPage.number - 1 : 0;
        const pageSize = this.userPage ? this.userPage.size : 10;

        const pageRequestParams = new PageRequestParams(prevPage, pageSize);
        this.getInstance().getUsers(pageRequestParams);
    }

    nextPage() {
        const nextPage = this.userPage ? this.userPage.number + 1 : 0;
        const pageSize = this.userPage ? this.userPage.size : 10;

        const pageRequestParams = new PageRequestParams(nextPage, pageSize);
        this.getInstance().getUsers(pageRequestParams);
    }

    updatePageSize(newSize) {
        const currPage = this.userPage ? this.userPage.number : 0;

        const pageRequestParams = new PageRequestParams(currPage, newSize);
        this.getInstance().getUsers(pageRequestParams);
    }


    onReloadCurrentPage() {
        const currPage = this.userPage ? this.userPage.number : 0;
        const pageSize = this.userPage ? this.userPage.size : 10;

        const pageRequestParams = new PageRequestParams(currPage, pageSize);
        this.getInstance().getUsers(pageRequestParams);
    }

    onDeleteRequest(id) {
        this.getInstance().deleteRequest(id);
    }

    onDeleteRequestSuccess() {
        this.onReloadCurrentPage();
    }

    onDeleteRequestError(err) {
        this.onReloadCurrentPage();
        this.lastErrorCode = err.response.status;
        this.lastErrorKey = "userErrorModal.delete";
    }



    onSaveNewRequest(user) {
        this.getInstance().saveNewRequest(user);
    }

    onSaveNewRequestSuccess() {
        this.onReloadCurrentPage();
    }

    onSaveNewRequestError(err) {
        this.onReloadCurrentPage();
        this.lastErrorCode = err.response.status;
        this.lastErrorKey = "userErrorModal.new";
    }



    onSaveEditRequest(parmas) {
        this.getInstance().saveEditRequest(parmas);
    }

    onSaveEditRequestSuccess() {
        this.onReloadCurrentPage();
    }

    onSaveEditRequestError(err) {
        this.onReloadCurrentPage();
        this.lastErrorCode = err.response.status;
        this.lastErrorKey = "userErrorModal.edit";
    }



    onSavePassword(userParams) {
        this.getInstance().savePassword(userParams);
    }

    onSavePasswordSuccess() {
        this.onReloadCurrentPage();
    }

    onSavePasswordError(err) {
        console.log("error changing password", err);
    }

    getUsers() {
        this.waitFor(UserSearchParamStore);

        const currPage = 0;
        const pageSize = this.userPage ? this.userPage.size : 10;

        const pageRequestParams = new PageRequestParams(currPage, pageSize);

        this.getInstance().getUsers(pageRequestParams);
        this.isSearching = true;
    }

    getUsersSuccess(users) {
        this.userPage = UserPage.toUserPage(users);
        this.isSearching = false;
    }

    getUsersError() {
        this.lastErrorKey = "userErrorModal.load";
        this.lastErrorCode = err.response.status;
    }
}

export default alt.createStore(UserStore, 'UserStore');
