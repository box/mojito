import _ from "lodash";
import React from "react";
import {FormattedMessage, FormattedNumber} from 'react-intl';
import { History } from "react-router";

import LocaleSelectorModal from "./LocaleSelectorModal";
import Locales from "../../utils/Locales";

import RepositoryActions from "../../actions/RepositoryActions";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchConstants from "../../utils/SearchConstants";

import {Navbar, Nav, NavItem, NavDropdown, MenuItem, Glyphicon, FormControl} from 'react-bootstrap';

import {Router, Link} from "react-router";

let Header = React.createClass({

    mixins: [History],

    getInitialState() {
        return {
            showLocaleSelectorModal: false
        };
    },

    logoutSelected: function () {
        this.refs.logoutForm.submit();
    },

    settingsSelected: function () {
        location.href = "/settings";
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
            "changedParam": SearchConstants.UPDATE_ALL
        });
    },

    render: function () {
        return (
            <Navbar fluid={true}>
                <a className="navbar-brand">
                    <img src="/img/logo.png" className="logo" alt="Box Mojito" />
                </a>
                <Nav>
                    <li className={(this.history.isActive("/repositories")) ? "active" : ""}>
                        <Link onClick={RepositoryActions.getAllRepositories} to="/repositories"><FormattedMessage id="header.repositories" /></Link>
                    </li>
                    <li className={(this.history.isActive("/workbench")) ? "active" : ""}>
                        <Link onClick={this.updateSearchParamsForDefaultView} to="/workbench"><FormattedMessage id="header.workbench" /></Link>
                    </li>
                    <li className={(this.history.isActive("/project-requests")) ? "active" : ""}>
                        <Link onClick={this.updateSearchParamsForDefaultView} to="/project-requests"><FormattedMessage id="header.projectRequests" /></Link>
                    </li>
                </Nav>
                <Nav pullRight={true}>
                    <NavDropdown title={USERNAME} id="user-menu">
                        <MenuItem onSelect={this.openLocaleSelectorModal}>
                            <Glyphicon glyph="globe" /> {Locales.getCurrentLocaleDisplayName()}
                            <LocaleSelectorModal key={_.uniqueId()} show={this.state.showLocaleSelectorModal} onClose={this.closeLocaleSelectorModal} />
                        </MenuItem>

                        <MenuItem onSelect={this.settingsSelected}>
                            <Glyphicon glyph="wrench" /> <FormattedMessage id="header.settings" />
                        </MenuItem>

                        <MenuItem divider />
                        <MenuItem onSelect={this.logoutSelected}>
                            <form action="/logout" method="post" ref="logoutForm">
                                <FormControl type="hidden" name="_csrf" value={CSRF_TOKEN}/>
                                <FormattedMessage id="header.loggedInUser.logout" />
                            </form>
                        </MenuItem>
                    </NavDropdown>
                </Nav>
            </Navbar>
        );
    }
});

export default Header;
