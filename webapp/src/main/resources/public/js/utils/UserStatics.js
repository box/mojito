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
        return "PM";
    }
    static authorityAdmin() {
        return "ADMIN";
    }
    static authorityTranslator() {
        return "TRANSLATOR";
    }
    static authorityUser() {
        return "USER";
    }
}

export default UserStatics;
