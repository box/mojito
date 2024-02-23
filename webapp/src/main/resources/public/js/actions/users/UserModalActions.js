import alt from "../../alt";

class UserModalActions {
    constructor() {
        this.generateActions(
            "resetUserModal",
            "updateUsername",
            "updateGivenName",
            "updateSurname",
            "updateCommonName",
            "updatePassword",
            "updatePasswordValidation",
            "updateRole",
            "toggleInfoAlert",
            "showValueAlert",
            "checkUsernameTaken",
            "checkUsernameTakenSuccess",
            "pushCurrentLocale",
            "removeLocaleFromList",
            "updateCanTranslateAllLocales",
            "updateLocaleFilter",
            "updateSelectedLocale",
        );
    }
}

export default alt.createActions(UserModalActions);
