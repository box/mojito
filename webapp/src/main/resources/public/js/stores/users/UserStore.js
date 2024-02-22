import alt from "../../alt";
import UserActions from "../../actions/users/UserActions";
import UserDataSource from "../../actions/users/UserDataSource";
import User from "../../sdk/entity/User";
import UserPage from "../../sdk/UsersPage";
import UserStatics from "../../utils/UserStatics";
import PageRequestParams from "../../sdk/PageRequestParams";

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

        /** @type {Boolean} */
        this.currentUsernameTaken = false;

        /** @type {Boolean} */
        this.currentUsernameChecked = false;

        this.bindActions(UserActions);
        this.registerAsync(UserDataSource);
    }

    onCloseUserModal() {
        this.currentUser = new User();
        this.modalState = UserStatics.stateHidden();
        this.showDeleteUserModal = false;
        this.lastErrorKey = null;
        this.currentUsernameTaken = false;
        this.currentUsernameChecked = false;
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
        this.getInstance().getAllUsers(pageRequestParams);
    }

    nextPage() {
        const nextPage = this.userPage ? this.userPage.number + 1 : 0;
        const pageSize = this.userPage ? this.userPage.size : 10;

        const pageRequestParams = new PageRequestParams(nextPage, pageSize);
        this.getInstance().getAllUsers(pageRequestParams);
    }

    updatePageSize(newSize) {
        console.log(newSize);
        const currPage = this.userPage ? this.userPage.number : 0;

        const pageRequestParams = new PageRequestParams(currPage, newSize);
        this.getInstance().getAllUsers(pageRequestParams);
    }


    onReloadCurrentPage() {
        const currPage = this.userPage ? this.userPage.number : 0;
        const pageSize = this.userPage ? this.userPage.size : 10;

        const pageRequestParams = new PageRequestParams(currPage, pageSize);
        this.getInstance().getAllUsers(pageRequestParams);
    }

    onGetAllUsers(pageRequestParams) {
        this.getInstance().getAllUsers(pageRequestParams);
    }

    /**
     * @param {UserPage} userPage
     */
    onGetAllUsersSuccess(userPage) {
        this.userPage = userPage;
    }

    onGetAllUsersError(err) {
        this.lastErrorKey = "userErrorModal.load";
        this.lastErrorCode = err.response.status;
        console.log("error fetching users", err);
    }



    onCheckUsernameTaken(username) {
        this.currentUsernameChecked = false;
        if (!username) {
            return;
        }
        this.getInstance().checkUsernameTakenRequest(username);
    }

    onCheckUsernameTakenSuccess(isTaken) {
        this.currentUsernameTaken = isTaken;
        this.currentUsernameChecked = true;
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

    onSavePasswordSuccess(updatedUser) {
        this.onReloadCurrentPage();
    }

    onSavePasswordError(err) {
        console.log("error changing password", err);
    }
}

export default alt.createStore(UserStore, 'UserStore');
