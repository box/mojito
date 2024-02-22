import React from "react";
import {injectIntl, __esModule} from "react-intl";
import UserMainPage from "./UserMainPage";
import AltContainer from "alt-container";
import UserStore from "../../stores/users/UserStore";

class UserManagement extends React.Component {
    render() {
        return (
            <AltContainer store={UserStore}>
                <UserMainPage />
            </AltContainer>
        );
    }
}

export default injectIntl(UserManagement);
