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



    onReloadCurrentPage() {
        if (!this.userPage) {
            return;
        }
        const pageRequestParams = new PageRequestParams(this.userPage.number, this.userPage.size);
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

    onDeleteRequestError(err) {
        console.log(err);
    }



    onDeleteRequest(id) {
        this.getInstance().deleteRequest(id);
    }

    onDeleteRequestSuccess() {
        this.onReloadCurrentPage();
    }

    onDeleteRequestError() {
        this.onReloadCurrentPage();
        this.lastErrorKey = "userErrorModal.delete";
    }



    onSaveNewRequest(user) {
        this.getInstance().saveNewRequest(user);
    }

    onSaveNewRequestSuccess() {
        this.onReloadCurrentPage();
    }

    onSaveNewRequestError() {
        this.onReloadCurrentPage();
        this.lastErrorKey = "userErrorModal.new";
    }



    onSaveEditRequest(parmas) {
        this.getInstance().saveEditRequest(parmas);
    }

    onSaveEditRequestSuccess() {
        this.onReloadCurrentPage();
    }

    onSaveEditRequestError() {
        this.onReloadCurrentPage();
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
