import React from "react";
import {injectIntl, FormattedMessage} from "react-intl";
import {Nav, NavItem} from "react-bootstrap";
import {LinkContainer} from "react-router-bootstrap";

class Settings extends React.Component {
    render() {
        return (
            <div>
                <h4 className="mbm"><FormattedMessage id="header.settings"/></h4>
                <Nav bsStyle="tabs" className="mbl">
                    <LinkContainer to="/settings/user-management">
                        <NavItem>
                            <FormattedMessage id="header.user-management"/>
                        </NavItem>
                    </LinkContainer>
                    <LinkContainer to="/settings/box">
                        <NavItem>Box Integration</NavItem>
                    </LinkContainer>
                </Nav>
                {this.props.children}
            </div>
        );
    }
}

export default injectIntl(Settings);
