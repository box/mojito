import alt from "../../alt.js";

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
            "checkUsernameTaken",
            "checkUsernameTakenSuccess",
        );
    }
}

export default alt.createActions(UserModalActions);
