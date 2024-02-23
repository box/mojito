import React from "react";
import {injectIntl, __esModule, FormattedMessage} from "react-intl";
import UserMainPage from "./UserMainPage";
import AltContainer from "alt-container";
import UserStore from "../../stores/users/UserStore";
import AuthorityService from "../../utils/AuthorityService";

class UserManagement extends React.Component {
    render() {
        if (!AuthorityService.hasPermissionsForUserManagement()) {
            return (
                <div className="ptl">
                    <h3 className="text-center mtl"><FormattedMessage id="users.forbidden"/></h3>
                </div>
            )
        }

        return (
            <AltContainer store={UserStore}>
                <UserMainPage />
            </AltContainer>
        );
    }
}

export default injectIntl(UserManagement);
