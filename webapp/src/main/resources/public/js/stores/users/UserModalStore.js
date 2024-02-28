import alt from "../../alt";
import UserModalActions from "../../actions/users/UserModalActions";
import UserDataSource from "../../actions/users/UserDataSource";
import UserStore from "./UserStore";

class UserModalStore {
    constructor() {

        /** @type {String} */
        this.username = '';

        /** @type {String} */
        this.givenName = '';

        /** @type {String} */
        this.surname = '';

        /** @type {String} */
        this.commonName = '';

        /** @type {String} */
        this.password = '';

        /** @type {String} */
        this.passwordValidation = '';

        /** @type {String} */
        this.role = '';

        /** @type {Boolean} */
        this.infoAlert = false;

        /** @type {Boolean} */
        this.currentUsernameTaken = false;

        /** @type {Boolean} */
        this.currentUsernameChecked = false;

        this.bindActions(UserModalActions);
        this.registerAsync(UserDataSource);
    }

    resetUserModal() {
        const store = UserStore.getState();

        this.username = store.currentUser.username;
        this.givenName = store.currentUser.givenName;
        this.surname = store.currentUser.surname;
        this.commonName = store.currentUser.commonName;
        this.password = '';
        this.passwordValidation = '';
        this.role = store.currentUser.getRole();
        this.infoAlert = false;
        this.currentUsernameTaken = false;
        this.currentUsernameChecked = false;
    }

    updateUsername(username) {
        this.username = username;
        this.checkUsernameTaken(username);
    }

    updateGivenName(givenName) {
        this.givenName = givenName;
    }

    updateSurname(surname) {
        this.surname = surname;
    }

    updateCommonName(commonName) {
        this.commonName = commonName;
    }

    updatePassword(pw) {
        this.password = pw;
    }

    updatePasswordValidation(pw) {
        this.passwordValidation = pw;
    }

    updateRole(role) {
        this.role = role;
    }

    toggleInfoAlert() {
        this.infoAlert = !this.infoAlert;
    }

    checkUsernameTaken(username) {
        this.currentUsernameChecked = false;
        if (!username) {
            return;
        }
        this.getInstance().checkUsernameTakenRequest(username);
    }

    checkUsernameTakenSuccess(isTaken) {
        this.currentUsernameTaken = isTaken;
        this.currentUsernameChecked = true;
    }
}

export default alt.createStore(UserModalStore, 'UserModalStore');
