import React from "react";
import {FormattedMessage, injectIntl, __esModule} from "react-intl";
import UserStatics from "../../utils/UserStatics";

function roleToIntlKey(role) {
    switch (role) {
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

class UserRoleRaw extends React.Component {
    render() {
        const role = this.props.user == null ? this.props.role : this.props.user.getRole();
        const roleKey = roleToIntlKey(role);

        if (roleKey == null) {
            return role;
        }

        return (<FormattedMessage id={roleKey}/>);
    }
};

let UserRole = injectIntl(UserRoleRaw);

export {
    roleToIntlKey,
    UserRole,
};
