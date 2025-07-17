import React from "react";
import {FormattedMessage} from 'react-intl';
import {withRouter} from "react-router";
import {LinkContainer} from "react-router-bootstrap";

import LocaleSelectorModal from "./LocaleSelectorModal";
import Locales from "../../utils/Locales";
import UrlHelper from "../../utils/UrlHelper";

import ChangePasswordModal from "./ChangePasswordModal";

import RepositoryActions from "../../actions/RepositoryActions";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import ScreenshotsPageActions from "../../actions/screenshots/ScreenshotsPageActions";
import ScreenshotsRepositoryActions from "../../actions/screenshots/ScreenshotsRepositoryActions";
import SearchConstants from "../../utils/SearchConstants";

import {FormControl, Glyphicon, MenuItem, Nav, Navbar, NavDropdown, NavItem} from 'react-bootstrap';
import BranchesPageActions from "../../actions/branches/BranchesPageActions";
import {withAppConfig} from "../../utils/AppConfig";

class Header extends React.Component {
    state = {
        showLocaleSelectorModal: false,
        showChangePasswordModal: false,
    };

    logoutSelected = () => {
        this.refs.logoutForm.submit();
    };

    openLocaleSelectorModal = () => {
        this.setState({
                showLocaleSelectorModal: true
            }
        );
    };

    closeLocaleSelectorModal = () => {
        this.setState({
            showLocaleSelectorModal: false
        });
    };

    openChangePasswordModal = () => {
        this.setState({
            showChangePasswordModal: true
        });
    };

    closeChangePasswordModal = () => {
        this.setState({
            showChangePasswordModal: false
        });
    };

    /**
     * Update the Workbench search params to load the default view
     *
     * @param {number} repoId
     */
    updateSearchParamsForDefaultView = () => {
        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL_LOCATION_NONE
        });
    };

    getUsernameDisplay() {
        const user = this.props.appConfig.user;
        let usernameDisplay = user.username;

        if (user.givenName) {
            usernameDisplay = user.givenName;
        } else if (user.commonName) {
            usernameDisplay = user.commonName;
        }

        return usernameDisplay;
    }

    render() {
        return (
            <Navbar fluid={true}>
                <a className="navbar-brand">
                    <img src={require('../../../img/logo.png')} className="logo" alt="Box Mojito"/>
                </a>
                <Nav>
                    <LinkContainer to="/repositories" onClick={() => {
                        if (this.props.router.isActive("/repositories")) {
                            RepositoryActions.getAllRepositories();
                        }
                    }}>
                        <NavItem><FormattedMessage id="header.repositories"/></NavItem>
                    </LinkContainer>
                    <LinkContainer to="/workbench" onClick={() => {
                        if (this.props.router.isActive("/workbench")) {
                            this.updateSearchParamsForDefaultView();
                        }
                    }}><NavItem><FormattedMessage id="header.workbench"/></NavItem></LinkContainer>
                    <LinkContainer to="/project-requests"><NavItem><FormattedMessage
                        id="header.projectRequests"/></NavItem></LinkContainer>
                    <LinkContainer to="/branches" onClick={() => {
                        if (this.props.router.isActive("/branches")) {
                            BranchesPageActions.resetBranchesSearchParams();
                            RepositoryActions.getAllRepositories();
                            BranchesPageActions.getBranches();
                        }
                    }}>
                        <NavItem><FormattedMessage id="header.branches"/></NavItem>
                    </LinkContainer>
                    <LinkContainer to="/screenshots" onClick={() => {
                        if (this.props.router.isActive("/screenshots")) {
                            ScreenshotsPageActions.resetScreenshotSearchParams();
                            ScreenshotsRepositoryActions.getAllRepositories();
                        }
                    }}><NavItem><FormattedMessage id="header.screenshots"/></NavItem></LinkContainer>

                    <LinkContainer to="/jobs">
                        <NavItem>
                            <FormattedMessage id="header.jobs"/>
                        </NavItem>
                    </LinkContainer>
                </Nav>
                <Nav pullRight={true}>
                    <NavDropdown title={this.getUsernameDisplay()} id="user-menu">
                        <MenuItem onSelect={this.openLocaleSelectorModal}>
                            <Glyphicon glyph="globe"/> {Locales.getCurrentLocaleDisplayName()}
                        </MenuItem>

                        <LinkContainer to="/settings">
                            <MenuItem>
                                <Glyphicon glyph="wrench"/> <FormattedMessage id="header.settings"/>
                            </MenuItem>
                        </LinkContainer>

                        <MenuItem divider/>

                        <MenuItem onSelect={this.openChangePasswordModal}>
                            <Glyphicon glyph="pencil"/> <FormattedMessage id="header.change-pw"/>
                        </MenuItem>

                        {!APP_CONFIG.userMenuLogoutHidden && (
                            <MenuItem onSelect={this.logoutSelected}>
                                <form action={UrlHelper.getUrlWithContextPath("/logout")} method="post" ref="logoutForm">
                                    <FormControl type="hidden" name="_csrf" value={this.props.appConfig.csrfToken}/>
                                    <Glyphicon glyph="log-out"/> <FormattedMessage id="header.loggedInUser.logout"/>
                                </form>
                            </MenuItem>
                        )}
                    </NavDropdown>
                </Nav>
                <LocaleSelectorModal key="headerLocaleSelectorModal" show={this.state.showLocaleSelectorModal}
                                     onClose={this.closeLocaleSelectorModal}/>
                <ChangePasswordModal key="headerChangePasswordModal" show={this.state.showChangePasswordModal}
                                     onClose={this.closeChangePasswordModal}/>
            </Navbar>
        );
    }
}

export default withAppConfig(withRouter(Header));
