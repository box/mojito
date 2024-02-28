class UserStatics {
    // states are Used to decide how the User and TupGroup Model gets rendered
    static stateEdit() {
        return "EDIT";
    }
    static stateNew() {
        return "NEW";
    }
    static stateHidden() {
        return "HIDDEN";
    }

    // max length for Forms, values greater than 40 will block the request to the ws
    static modalFormMaxLength() {
        return 40;
    }

    // authorities returned from the WS
    static authorityPm() {
        return "ROLE_PM";
    }
    static authorityAdmin() {
        return "ROLE_ADMIN";
    }
    static authorityTranslator() {
        return "ROLE_TRANSLATOR";
    }
    static authorityUser() {
        return "ROLE_USER";
    }
}

export default UserStatics;
