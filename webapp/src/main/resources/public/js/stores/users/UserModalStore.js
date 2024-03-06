import alt from "../../alt";
import UserModalActions from "../../actions/users/UserModalActions";
import UserDataSource from "../../actions/users/UserDataSource";
import UserStore from "./UserStore";
import LocaleStore from "./LocaleStore";

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

        this.valueAlert = false;

        /** @type {Boolean} */
        this.currentUsernameTaken = false;

        /** @type {Boolean} */
        this.currentUsernameChecked = false;

        /** @type {Boolean} */
        this.canTranslateAllLocales = true;

        /** @type {String[]} */
        this.localeTags = [];

        /** @type {String} */
        this.localeInput = '';

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
        this.valueAlert = false;
        this.currentUsernameTaken = false;
        this.currentUsernameChecked = false;
        this.canTranslateAllLocales = store.currentUser.canTranslateAllLocales;
        this.localeTags = store.currentUser.getLocaleTags();
        this.localeInput = this.getAnyLocale();
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

    showValueAlert() {
        this.valueAlert = true;
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

    getAnyLocale() {
        const localeStore = LocaleStore.getState();
        for (let t of localeStore.allLocales.map((x) => x.bcp47Tag)) {
            if (!this.localeTags.includes(t)) {
                return t;
            }
        }
        return null;
    }

    pushCurrentLocale() {
        this.localeTags = this.localeTags.concat([this.localeInput]);
        this.localeInput = this.getAnyLocale();
    }

    removeLocaleFromList(tag) {
        const idx = this.localeTags.indexOf(tag);
        this.localeTags = this.localeTags.toSpliced(idx, 1);
    }

    updateCanTranslateAllLocales(state) {
        this.canTranslateAllLocales = state;
    }

    updateLocaleInput(localeInput) {
        this.localeInput = localeInput;
    }
}

export default alt.createStore(UserModalStore, 'UserModalStore');
