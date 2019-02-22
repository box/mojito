import _ from "lodash";
import React from "react";
import {FormattedMessage} from 'react-intl';
import {withRouter} from "react-router";
import {LinkContainer} from "react-router-bootstrap";

import LocaleSelectorModal from "./LocaleSelectorModal";
import Locales from "../../utils/Locales";
import UrlHelper from "../../utils/UrlHelper";

import RepositoryActions from "../../actions/RepositoryActions";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import ScreenshotsPageActions from "../../actions/screenshots/ScreenshotsPageActions";
import ScreenshotsRepositoryActions from "../../actions/screenshots/ScreenshotsRepositoryActions";
import SearchConstants from "../../utils/SearchConstants";

import {FormControl, Glyphicon, MenuItem, Nav, Navbar, NavDropdown, NavItem} from 'react-bootstrap';

import BranchesSearchParamsActions from "../../actions/branches/BranchesSearchParamsActions";
import UserHelper from "../../utils/UserHelper";

let Header = React.createClass({

    getInitialState() {
        return {
            showLocaleSelectorModal: false
        };
    },

    logoutSelected: function () {
        this.refs.logoutForm.submit();
    },

    settingsSelected: function () {
        location.href = UrlHelper.getUrlWithContextPath("/settings");
    },

    openLocaleSelectorModal: function () {
        this.setState({
                showLocaleSelectorModal: true
            }
        );
    },

    closeLocaleSelectorModal: function () {
        this.setState({
            showLocaleSelectorModal: false
        });
    },

    /**
     * Update the Workbench search params to load the default view
     *
     * @param {number} repoId
     */
    updateSearchParamsForDefaultView: function () {
        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "doNotTranslate": true
        });
    },


    render: function () {
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
                            BranchesSearchParamsActions.resetBranchesSearchParams();
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
                </Nav>
                <Nav pullRight={true}>
                    <NavDropdown title={UserHelper.getUsername()} id="user-menu">
                        <MenuItem onSelect={this.openLocaleSelectorModal}>
                            <Glyphicon glyph="globe"/> {Locales.getCurrentLocaleDisplayName()}
                            <LocaleSelectorModal key={_.uniqueId()} show={this.state.showLocaleSelectorModal}
                                                 onClose={this.closeLocaleSelectorModal}/>
                        </MenuItem>

                        <MenuItem onSelect={this.settingsSelected}>
                            <Glyphicon glyph="wrench"/> <FormattedMessage id="header.settings"/>
                        </MenuItem>

                        <MenuItem divider/>
                        <MenuItem onSelect={this.logoutSelected}>
                            <form action={UrlHelper.getUrlWithContextPath("/logout")} method="post" ref="logoutForm">
                                <FormControl type="hidden" name="_csrf" value={CSRF_TOKEN}/>
                                <FormattedMessage id="header.loggedInUser.logout"/>
                            </form>
                        </MenuItem>
                    </NavDropdown>
                </Nav>
            </Navbar>
        );
    }
});

export default withRouter(Header);
