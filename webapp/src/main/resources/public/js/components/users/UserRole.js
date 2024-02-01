import React from "react";
import {FormattedMessage, injectIntl, __esModule} from "react-intl";
import UserStatics from "../../utils/UserStatics";

let createClass = require('create-react-class');

function roleToIntlKey(role) {
    switch (role.replace('ROLE_', '')) {
        case UserStatics.authorityPm():
            return "users.role.pm";
        case UserStatics.authorityTranslator():
            return "users.role.translator";
        case UserStatics.authorityAdmin():
            return "users.role.admin";
        case UserStatics.authorityUser():
            return "users.role.user";
        default:
            return null;
    }
}

let UserRoleRaw = createClass({
    render() {
        const role = this.props.user == null ? this.props.role : this.props.user.getRole();
        const roleKey = roleToIntlKey(role);

        if (roleKey == null) {
            return role;
        }

        return (<FormattedMessage id={roleKey}/>);
    },
});

let UserRole = injectIntl(UserRoleRaw);

export {
    roleToIntlKey,
    UserRole,
};
