import _ from "lodash";
import React from "react";
import ReactIntl from 'react-intl';
import { History } from "react-router";

import LocaleSelectorModal from "./LocaleSelectorModal";
import Locales from "../../utils/Locales";

import RepositoryActions from "../../actions/RepositoryActions";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchConstants from "../../utils/SearchConstants";

import {Navbar, Nav, NavItem, DropdownButton, MenuItem, Glyphicon} from 'react-bootstrap';

import {Router, Link} from "react-router";


let {IntlMixin, FormattedMessage, FormattedNumber} = ReactIntl;


let Header = React.createClass({

    mixins: [IntlMixin, History],

    getInitialState() {
        return {
            showLocaleSelectorModal: false
        };
    },

    logoutSelected: function () {
        React.findDOMNode(this.refs.logoutForm).submit();
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
                        <Link onClick={RepositoryActions.getAllRepositories} to="/repositories">{this.getIntlMessage("header.repositories")}</Link>
                    </li>
                    <li className={(this.history.isActive("/workbench")) ? "active" : ""}>
                        <Link onClick={this.updateSearchParamsForDefaultView} to="/workbench">{this.getIntlMessage("header.workbench")}</Link>
                    </li>
                    <li className={(this.history.isActive("/project-requests")) ? "active" : ""}>
                        <Link onClick={this.updateSearchParamsForDefaultView} to="/project-requests">{this.getIntlMessage("header.projectRequests")}</Link>
                    </li>
                </Nav>
                <Nav right>
                    <DropdownButton title={USERNAME}>
                        <MenuItem onSelect={this.openLocaleSelectorModal}>
                            <Glyphicon glyph="globe" /> {Locales.getCurrentLocaleDisplayName()}
                            <LocaleSelectorModal key={_.uniqueId()} show={this.state.showLocaleSelectorModal} onClose={this.closeLocaleSelectorModal} />
                        </MenuItem>

                        <MenuItem onSelect={this.settingsSelected}>
                            <Glyphicon glyph="wrench" /> {this.getIntlMessage("header.settings")}
                        </MenuItem>

                        <MenuItem divider />
                        <MenuItem onSelect={this.logoutSelected}>
                            <form action="/logout" method="post" ref="logoutForm">
                                <input type="hidden" name="_csrf" value={CSRF_TOKEN}/>
                                <FormattedMessage message={this.getIntlMessage("header.loggedInUser.logout")} />
                            </form>
                        </MenuItem>

                    </DropdownButton>
                </Nav>
            </Navbar>
        );
    }
});

export default Header;
