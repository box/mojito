import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, FormControl, Modal, Form, FormGroup, ControlLabel, Alert, Collapse, Glyphicon, Dropdown, MenuItem, DropdownButton} from "react-bootstrap";
import UserStatics from "../../utils/UserStatics";
import UserActions from "../../actions/users/UserActions";
import UserModalActions from "../../actions/users/UserModalActions";
import {roleToIntlKey} from "./UserRole";
import AuthorityService from "../../utils/AuthorityService";

class UserModal extends React.Component{

    getUsernameFormValidationState() {
        if (this.props.user.modalState === UserStatics.stateEdit() || this.props.modal.username.length == 0) {
            return null;
        }
        if (this.props.modal.currentUsernameTaken || this.props.modal.username.length > UserStatics.modalFormMaxLength()) {
            return 'error';
        }
        return 'success';
    }

    getPasswordFormValidationState() {
        if (this.props.modal.password.length == 0 && this.props.modal.passwordValidation.length == 0) {
            return null;
        }
        if (this.props.modal.password.length > UserStatics.modalFormMaxLength() || this.props.modal.passwordValidation !== this.props.modal.password) {
            return 'error';
        }
        return 'success';
    }

    renderLocales() {
        const options = [];
        let freeTagList = []
        for (let tag of this.props.locales.allLocales.map((x) => x.bcp47Tag).toSorted()) {
            if (this.props.modal.localeTags.includes(tag) || (this.props.modal.localeFilter && !tag.toLowerCase().includes(this.props.modal.localeFilter.toLowerCase()))) {
                continue;
            }
            options.push(<option key={tag} value={tag} />);
            freeTagList.push(tag);
        }

        let localeElements = [];
        for (let tag of this.props.modal.localeTags.toSorted()) {
            localeElements.push(
                <Button key={tag + "-btn"} bsStyle="danger" onClick={() => UserModalActions.removeLocaleFromList(tag)}>
                    <span className="glyphicon glyphicon-trash foreWhite" aria-label={"remove locale " + tag}/>
                </Button>
            );
            localeElements.push(<span key={tag + "-display"}>{tag}</span>);
        }

        return (
            <div className="mtm users-locale-select-root">
                <div className="locales-list panel panel-default">
                    {localeElements}
                </div>
                <FormControl
                    onChange={(e) => UserModalActions.updateLocaleInput(e.target.value)}
                    type="text"
                    value={this.props.modal.localeInput}
                    style={{gridArea: 'new-select'}}
                    placeholder="de-DE"
                    id="new-locale-input"
                    list="new-locale-options"
                />
                <datalist id="new-locale-options">
                    {options}
                </datalist>
                <div style={{gridArea: "new-btn", justifySelf: "start"}}>
                    <Button bsStyle="primary" onClick={UserModalActions.pushCurrentLocale} disabled={!freeTagList.includes(this.props.modal.localeInput)}>
                        <span className="glyphicon glyphicon-plus foreWhite" aria-label="add locale for user"/>
                    </Button>
                </div>
            </div>
        );
    }

    renderForm() {
        let usernameForm = '';
        let optionalPasswordLabel = '';

        switch (this.props.user.modalState) {
            case UserStatics.stateNew():
                usernameForm = (
                    <div>
                        <div style={{display: "flex"}} className="pbs">
                            <ControlLabel className="mutedBold"><FormattedMessage id="userEditModal.form.label.username"/></ControlLabel>
                            <div style={{flexGrow: 1}}></div>
                            {this.props.modal.currentUsernameTaken && this.props.modal.username && <FormattedMessage id="userEditModal.form.label.usernameTaken"/>}
                        </div>
                        <FormControl
                            onChange={(e) => UserModalActions.updateUsername(e.target.value)}
                            type="text"
                            value={this.props.modal.username}
                            className="mbm"
                            placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.placeholder.username" })}
                        />
                    </div>
                );
                break;
            case UserStatics.stateEdit():
                optionalPasswordLabel = <span className="mutedBold mtm pbs pull-right">(optional)</span>;
                usernameForm = (
                    <div className="mutedBold">
                        <ControlLabel><FormattedMessage id="userEditModal.form.label.username"/>:</ControlLabel>
                        <span className="mls">{this.props.modal.username}</span>
                    </div>
                );
                break;
        }

        let options = []
        for (let i of [UserStatics.authorityUser(), UserStatics.authorityTranslator(), UserStatics.authorityPm(), UserStatics.authorityAdmin()]) {
            options.push(<option key={i} value={i}>{this.props.intl.formatMessage({id: roleToIntlKey(i)})}</option>)
        }

        const roleCanTranslate = AuthorityService.canEditTransalationRoles().includes(this.props.modal.role);
        const roleName = this.props.modal.role ? this.props.intl.formatMessage({id: roleToIntlKey(this.props.modal.role)}) : '';

        return (
            <Form>
                <FormGroup validationState={this.getUsernameFormValidationState()} controlId="username Form">{usernameForm}</FormGroup>
                <FormGroup controlId="name Form">
                    <ControlLabel className="mutedBold pbs"><FormattedMessage id="userEditModal.form.label.givenName"/></ControlLabel>
                    <FormControl
                        onChange={(e) => UserModalActions.updateGivenName(e.target.value)}
                        type="text"
                        value={this.props.modal.givenName || ''}
                        placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.placeholder.givenName" })}
                    />

                    <ControlLabel className="mutedBold mtm pbs"><FormattedMessage id="userEditModal.form.label.surname"/></ControlLabel>
                    <FormControl
                        onChange={(e) => UserModalActions.updateSurname(e.target.value)}
                        type="text"
                        value={this.props.modal.surname || ''}
                        placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.placeholder.surname" })}
                    />

                    <ControlLabel className="mutedBold mtm pbs"><FormattedMessage id="userEditModal.form.label.commonName"/></ControlLabel>
                    <FormControl
                        onChange={(e) => UserModalActions.updateCommonName(e.target.value)}
                        type="text"
                        value={this.props.modal.commonName || ''}
                        placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.placeholder.commonName" })}
                    />
                </FormGroup>
                <hr style={{marginBottom: "0px"}}/>
                <FormGroup validationState={this.getPasswordFormValidationState()}>

                    <ControlLabel className="mutedBold mtm pbs">
                        <FormattedMessage id="userEditModal.form.label.password"/>
                    </ControlLabel>
                    {optionalPasswordLabel}
                    <FormControl
                        id="password input"
                        onChange={(e) => UserModalActions.updatePassword(e.target.value)}
                        type="password"
                        value={this.props.modal.password || ''}
                        placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.placeholder.password" })}
                    />
                    <FormControl
                        id="password validation input"
                        className="mtm"
                        onChange={(e) => UserModalActions.updatePasswordValidation(e.target.value)}
                        type="password"
                        value={this.props.modal.passwordValidation || ''}
                        placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.placeholder.passwordValidation" })}
                    />
                </FormGroup>
                <hr/>
                <FormGroup>
                    <input
                        id="canTranslateAllLocales"
                        onChange={(e) => UserModalActions.updateCanTranslateAllLocales(e.target.checked)}
                        type="checkbox"
                        checked={this.props.modal.canTranslateAllLocales}
                        value={<FormattedMessage id="userEditModal.form.canTranslateAllLocales"/>}
                        disabled={!roleCanTranslate}
                    />
                    <label className={"mls" + (roleCanTranslate ? "" : " text-muted")} htmlFor="canTranslateAllLocales"><FormattedMessage id="userEditModal.form.canTranslateAllLocales"/></label>
                    {!roleCanTranslate && <div className="text-right pull-right"><FormattedMessage id="userEditModal.form.localesDisabled" values={{role: roleName}}/></div>}
                    {!this.props.modal.canTranslateAllLocales && roleCanTranslate && this.renderLocales()}
                </FormGroup>
                <hr style={{marginBottom: "0px"}}/>
                <FormGroup>
                    <ControlLabel className="mutedBold mtm pbs"><FormattedMessage id="userEditModal.form.label.authority"/></ControlLabel>

                    <div style={{display: "flex", gap: "10px", alignItems: "center"}}>
                        <FormControl
                            onChange={(e) => UserModalActions.updateRole(e.target.value)}
                            componentClass="select"
                            placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.select.placeholder" })}
                            style={{flexGrow: 1}}
                            value={this.props.modal.role}
                        >
                            {options}
                        </FormControl>
                        <Glyphicon
                            className="clickable"
                            style={{fontSize: "20px"}}
                            glyph="glyphicon glyphicon-info-sign"
                            onClick={UserModalActions.toggleInfoAlert}
                        />
                    </div>
                    <Collapse in={this.props.modal.infoAlert} className="mtm">
                        <div>
                            <Alert bsStyle="info">
                                <p className="text-muted brand-font-weight"><FormattedMessage id="users.userInformationAlert"/></p>
                            </Alert>
                        </div>
                    </Collapse>
                </FormGroup>
            </Form>
        );
    }

    isUsernameTaken() {
        if (this.props.user.modalState !== UserStatics.stateNew())
            return false;
        else
            return !(this.props.modal.currentUsernameChecked && !this.props.modal.currentUsernameTaken);
    }

    arePasswordStatesWrong() {
        let passwordValue = this.props.modal.password;
        let passwordValidationValue = this.props.modal.passwordValidation;
        let isPasswordLengthExceeded = passwordValue.length > UserStatics.modalFormMaxLength() || passwordValidationValue.length > UserStatics.modalFormMaxLength();
        let isPasswordEmpty = passwordValidationValue.length <= 0 || passwordValue.length <= 0;

        // entering a password on State "EDIT" is optional
        if (this.props.user.modalState === UserStatics.stateEdit()) {
            return passwordValue !== passwordValidationValue || isPasswordLengthExceeded;
        } else if (this.props.user.modalState === UserStatics.stateNew()) {
            return passwordValue !== passwordValidationValue || isPasswordEmpty || isPasswordLengthExceeded;
        } else
            return false;
    }

    valueCheckerPassed() {
        const usernameAlreadyTaken = this.isUsernameTaken();
        const passwordCheckFailed = this.arePasswordStatesWrong();
        const usernameLength = this.props.modal.username.length;

        return !(usernameAlreadyTaken || !(this.props.modal.username && this.props.modal.username.trim()) || passwordCheckFailed || usernameLength > UserStatics.modalFormMaxLength());
    }

    onUserEditModalSaveClicked() {
        if (this.valueCheckerPassed()) {
            let savedUser = {
                "username": (this.props.modal.username || '').trim(),
                "givenName": (this.props.modal.givenName || '').trim(),
                "surname": (this.props.modal.surname || '').trim(),
                "commonName": (this.props.modal.commonName || '').trim(),
                "password": this.props.modal.password,
                "canTranslateAllLocales": this.props.modal.canTranslateAllLocales,
                "userLocales": this.props.modal.localeTags.map((x) => {return {"locale": {"bcp47Tag": x}};}),
                "authorities": [{
                    "authority": this.props.modal.role
                }],
            };

            UserActions.closeUserModal();

            switch (this.props.user.modalState) {
                case UserStatics.stateEdit():
                    UserActions.saveEditRequest({user: savedUser, id: this.props.user.currentUser.id});
                    break;
                case UserStatics.stateNew():
                    UserActions.saveNewRequest(savedUser);
                    break;
            }
        } else {
            UserModalActions.showValueAlert();
        }
    }

    modalTitle() {
        let modalStateTitle = null;
        switch (this.props.user.modalState) {
            case UserStatics.stateEdit() :
                modalStateTitle = <FormattedMessage id="userEditModal.edit.title"/>;
                break;
            case UserStatics.stateNew() :
                modalStateTitle = <FormattedMessage id="userEditModal.new.title"/>;
                break;
            default :
                modalStateTitle = this.props.user.modalState;
        }

        return (
            <span className="mutedBold">{modalStateTitle}</span>
        );
    }

    renderValueAlert() {
        return (
            <Collapse in={this.props.modal.valueAlert}>
                <div>
                    <Alert bsStyle="danger">
                        <p className="text-center text-muted"><FormattedMessage id="userEditModal.alertMessage"/></p>
                    </Alert>
                </div>
            </Collapse>
        );
    }

    render() {
        return (
            <div>
                <Modal bsSize="large" show={this.props.user.modalState != UserStatics.stateHidden()} onHide={UserActions.closeUserModal}>
                    <Modal.Header closeButton>
                        <Modal.Title>{this.modalTitle()}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        {this.renderValueAlert()}
                        {this.renderForm()}
                    </Modal.Body>
                    <Modal.Footer>
                        <div className="text-center mbm">
                            <Button bsStyle="primary" onClick={() => this.onUserEditModalSaveClicked()} disabled={!this.valueCheckerPassed()}>
                                <FormattedMessage id="label.save"/>
                            </Button>
                            <Button onClick={UserActions.closeUserModal}>
                                <FormattedMessage id="label.cancel"/>
                            </Button>
                        </div>
                    </Modal.Footer>
                </Modal>
            </div>
        );
    }
}

export default injectIntl(UserModal);
