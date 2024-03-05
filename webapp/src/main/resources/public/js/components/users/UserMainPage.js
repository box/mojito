import React from "react";
import {FormattedMessage, FormattedDate, FormattedTime, injectIntl, __esModule} from "react-intl";
import {Table, Button, OverlayTrigger, Popover, DropdownButton, MenuItem} from "react-bootstrap";
import UserActions from "../../actions/users/UserActions";
import UserStore from "../../stores/users/UserStore";
import User from "../../sdk/entity/User";
import UserModal from "./UserModal";
import UserDeleteModal from "./UserDeleteModal";
import { UserRole } from "./UserRole";
import UserErrorModal from "./UserErrorModal";
import AltContainer from "alt-container";
import UserModalStore from "../../stores/users/UserModalStore";
import UserModalActions from "../../actions/users/UserModalActions";

class UserMainPage extends React.Component {

    componentDidMount() {
        UserActions.reloadCurrentPage();
    }

    mutedText(content) {
        return (
            <span className="text-muted">{content}</span>
        );
    }

    numPages() {
        return this.props.userPage == null ? 1 : this.props.userPage.totalPages;
    }

    numUsers() {
        return this.props.userPage == null ? 0 : this.props.userPage.totalElements;
    }

    pageSize() {
        return this.props.userPage == null ? 10 : this.props.userPage.size;
    }

    currentPage() {
        return this.props.userPage == null ? 1 : this.props.userPage.number + 1;
    }

    renderPageBar() {
        let items = [];
        for (let i of [10, 25, 50, 100]) {
            items.push(
                <MenuItem key={i} eventKey={i} active={i == this.pageSize()} onSelect={(s, _) => UserActions.updatePageSize(s)}>
                    {i}
                </MenuItem>
            );
        }

        const title = <FormattedMessage values={{"pageSize": this.pageSize()}} id="users.usersPerPage" />;

        return (
            <div style={{gridArea: "pages", justifySelf: "end"}}>
                <DropdownButton id="users-per-page" title={title} className="mrl">
                    {items}
                </DropdownButton>
                <Button
                    bsSize="small"
                    disabled={this.currentPage() <= 1}
                    onClick={UserActions.prevPage}
                >
                    <span className="glyphicon glyphicon-chevron-left"></span>
                </Button>
                <label className="mls mrs default-label current-pageNumber">
                    {this.currentPage()}
                </label>
                <Button
                    bsSize="small"
                    disabled={this.currentPage() >= this.numPages()}
                    onClick={UserActions.nextPage}
                >
                    <span className="glyphicon glyphicon-chevron-right"></span>
                </Button>
                <Button className="mlm btnCreate" bsSize="small" bsStyle="primary" onClick={() => this.openNewUserModal()}>
                    <FormattedMessage id="users.buttons.newUser"/>
                </Button>
            </div>
        );
    }

    openEditUserModal(user) {
        UserActions.openEditUserModal(user);
        UserModalActions.resetUserModal();
    }

    openNewUserModal() {
        UserActions.openNewUserModal();
        UserModalActions.resetUserModal();
    }

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
                <Button bsStyle="primary" bsSize="small" className= "mlxs" onClick={() => this.openEditUserModal(user)}>
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
    }

    renderUsersTable() {
        let rows = [];

        /** @type {User[]} */
        let users = this.props.userPage == null ? [] : this.props.userPage.content;

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
    }

    render() {
        return (
            <div className="users-root mll mrl">
                <div style={{gridArea: "count", justifySelf: "start"}} className="mbl">
                    <h4><FormattedMessage values={{numUsers: this.numUsers()}} id="users.count" /></h4>
                </div>
                {this.renderPageBar()}
                {this.renderUsersTable()}
                <AltContainer stores={{user: UserStore, modal: UserModalStore}}>
                    <UserModal />
                </AltContainer>
                <AltContainer store={UserStore}>
                    <UserDeleteModal
                        onClose={() => UserActions.closeUserModal()}
                        onDeleteUserConfirmed={(userId) => {
                            UserActions.closeUserModal();
                            UserActions.deleteRequest(userId);
                            UserActions.reloadCurrentPage();
                        }}
                    />
                </AltContainer>
                <AltContainer store={UserStore}>
                    <UserErrorModal onClose={() => UserActions.closeUserModal()} />
                </AltContainer>
            </div>
        );
    }

}

export default injectIntl(UserMainPage);
