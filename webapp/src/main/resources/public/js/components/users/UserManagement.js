import React from "react";
import {FormattedMessage, FormattedDate, FormattedTime, injectIntl, __esModule} from "react-intl";
import {
    Table, Button, OverlayTrigger, Popover,
    DropdownButton, MenuItem
} from "react-bootstrap";
import FluxyMixin from "alt-mixins/FluxyMixin";
import UserStore from "../../stores/users/UserStore";
import UserActions from "../../actions/users/UserActions";
import User from "../../sdk/entity/User";
import PageRequestParams from "../../sdk/PageRequestParams";
import UserStatics from "../../utils/UserStatics";
import UserModal from "./UserModal";
import UserDeleteModal from "./UserDeleteModal";
import { UserRole } from "./UserRole";
import UserErrorModal from "./UserErrorModal";


let createClass = require('create-react-class');

let UserManagement = createClass({
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onUserStoreChanged": UserStore,
        }
    },

    componentDidMount() {
        this.reloadUsers();
    },

    getInitialState: function () {
        return {
            currentPageNumber: 1,
            maxPageNumber: 1,
            pageSize: 10,
            modalState: false,
            store: UserStore.getState(),
        };
    },

    onUserStoreChanged () {
        const store = UserStore.getState();
        this.setState({
            store: store,
            maxPageNumber: store.userPage == null ? 1 : store.userPage.totalPages,
        });
    },

    numPages() {
        return this.state.store.userPage == null ? 1 : this.state.store.userPage.totalPages;
    },

    numUsers() {
        return this.state.store.userPage == null ? 0 : this.state.store.userPage.totalElements;
    },

    reloadUsers() {
        const pageRequestParams = new PageRequestParams(this.state.currentPageNumber - 1, this.state.pageSize);
        UserActions.getAllUsers(pageRequestParams);
    },

    onFetchPreviousPageClicked() {
        this.state.currentPageNumber -= 1;
        this.reloadUsers();
    },

    onFetchNextPageClicked() {
        this.state.currentPageNumber += 1;
        this.reloadUsers();
    },

    onSelectionChanged(e) {
        this.state.pageSize = e;
        this.reloadUsers();
    },

    renderPageBar() {
        let items = [];
        for (let i of [10, 25, 50, 100]) {
            items.push(
                <MenuItem key={i} eventKey={i} active={i == this.state.pageSize} onSelect={(x) => this.setState({pageSize: x})}>
                    {i}
                </MenuItem>
            );
        }

        const title = <FormattedMessage values={{"pageSize": this.state.pageSize}} id="users.usersPerPage" />;

        return (
            <div style={{gridArea: "pages", justifySelf: "end"}}>
                <DropdownButton id="users-per-page" title={title} className="mrl" onSelect={this.onSelectionChanged}>
                    {items}
                </DropdownButton>
                <Button
                    bsSize="small"
                    disabled={this.state.currentPageNumber <= 1}
                    onClick={this.onFetchPreviousPageClicked}
                >
                    <span className="glyphicon glyphicon-chevron-left"></span>
                </Button>
                <label className="mls mrs default-label current-pageNumber">
                    {this.state.currentPageNumber}
                </label>
                <Button
                    bsSize="small"
                    disabled={this.state.currentPageNumber >= this.numPages()}
                    onClick={this.onFetchNextPageClicked}
                >
                    <span className="glyphicon glyphicon-chevron-right"></span>
                </Button>
            </div>
        );
    },

    /**
     * @param {User} user
     */
    renderEditButtons(user) {
        let deleteTitle = "delete";
        let deleteBtnOverlay = <Popover id="deleteBtnOverlay"><FormattedMessage id="users.buttons.delete.tooltip"/></Popover>;
        let modalBtnOverlay = <Popover id="modalBtnOverlay"><FormattedMessage id="users.buttons.edit.tooltip"/></Popover>;

        let deleteButton = '';
        let modalButton = '';

        if (user.username != APP_CONFIG.user.username) {
            deleteButton = (
                <OverlayTrigger placement="top" overlay={deleteBtnOverlay}>
                    <Button bsStyle="primary" bsSize="small" onClick={() => UserActions.openDeleteUserModal(user)}>
                        <span className="glyphicon glyphicon-remove forceWhite" aria-label={deleteTitle}/>
                    </Button>
                </OverlayTrigger>
            );
        }

        modalButton = (
            <OverlayTrigger placement="top" overlay={modalBtnOverlay}>
                <Button bsStyle="primary" bsSize="small" className= "mlxs" onClick={() => UserActions.openEditUserModal(user)}>
                    <span className="glyphicon glyphicon-pencil forceWhite" aria-label={deleteTitle}/>
                </Button>
            </OverlayTrigger>
        );

        return (
            <div className="prs pts pbs pull-right">
                {deleteButton}
                {modalButton}
            </div>
        );
    },

    renderUsersTable() {
        let rows = [];

        /** @type {User[]} */
        let users = this.state.store.userPage == null ? [] : this.state.store.userPage.content;

        for (let u of users) {
            rows.push(
                <tr key={'user.table.' + u.username}>
                    <td>{u.username}</td>
                    <td>{u.getDisplayName()}</td>
                    <td><UserRole user={u} /></td>
                    <td><FormattedDate value={u.createdDate}/> <FormattedTime value={u.createdDate}/></td>
                    <td>{this.renderEditButtons(u)}</td>
                </tr>
            );
        }

        return (
            <Table style={{gridArea: "main", width: "100%"}}>
                <thead>
                    <tr>
                        <th>{this.mutedText(<FormattedMessage id="users.table.header.username"/>)}</th>
                        <th>{this.mutedText(<FormattedMessage id="users.table.header.name"/>)}</th>
                        <th>{this.mutedText(<FormattedMessage id="users.table.header.authority"/>)}</th>
                        <th>{this.mutedText(<FormattedMessage id="users.table.header.dateCreated"/>)}</th>
                        <th>{this.mutedText(<FormattedMessage id="users.table.header.controls"/>)}</th>
                    </tr>
                </thead>
                <tbody>{rows}</tbody>
            </Table>
        );
    },

    render() {
        let popover = (
            <Popover id="popover-trigger-hover-focus">
                <span className="text-muted brand-font-weight"><FormattedMessage id="users.buttons.newUser"/></span>
            </Popover>
        );

        return (
            <div className="users-root mll mrl">
                <div style={{gridArea: "count", justifySelf: "start"}} className="mbl">
                    <h4><FormattedMessage values={{numUsers: this.numUsers()}} id="users.count" /></h4>
                </div>
                {this.renderPageBar()}
                {this.renderUsersTable()}
                <div style={{gridArea: "newUser", justifySelf: "start"}}>
                    <OverlayTrigger overlay={popover}>
                        <Button className="btnCreate" bsStyle="primary" onClick={() => UserActions.openNewUserModal()}>
                            <span className="glyphicon glyphicon-plus foreWhite" aria-label="add user"/>
                        </Button>
                    </OverlayTrigger>
                </div>
                {this.state.store.modalState != UserStatics.stateHidden() && <UserModal />}
                <UserDeleteModal />
                <UserErrorModal />
            </div>
        );
    },

    mutedText(content) {
        return (
            <span className="text-muted">{content}</span>
        );
    },
});

export default injectIntl(UserManagement);
