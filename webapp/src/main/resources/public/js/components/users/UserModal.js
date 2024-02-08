import React from "react";
import {FormattedMessage, intlShape, injectIntl} from "react-intl";
import {
    Popover, Button, FormControl, Modal, Form, FormGroup, ControlLabel, Alert, Collapse, Glyphicon, OverlayTrigger, Checkbox,
    Badge, Fade
} from "react-bootstrap";
import UserStatics from "../../utils/UserStatics";
import FluxyMixin from "alt-mixins/FluxyMixin";
import UserStore from "../../stores/users/UserStore";
import UserActions from "../../actions/users/UserActions";
import {roleToIntlKey} from "./UserRole";


let createClass = require('create-react-class');

let UserModal = createClass({

    mixins: [FluxyMixin],

    propTypes: {
        intl: intlShape.isRequired,
    },

    statics: {
        storeListeners: {
            "onUserStoreChanged": UserStore,
        }
    },

    onUserStoreChanged() {
        this.setState({
            store: UserStore.getState(),
        });
    },

    getInitialState() {
        const store = UserStore.getState();
        return {
            store: store,
            usernameTaken: false,
            username: store.currentUser.username,
            givenName: store.currentUser.givenName,
            surname: store.currentUser.surname,
            commonName: store.currentUser.commonName,
            password: '',
            passwordValidation: '',
            role: store.currentUser.getRole(),
            infoAlert: false,
        };
    },

    getUsernameFormValidationState() {
        if (this.state.store.modalState === UserStatics.stateEdit() || this.state.username.length == 0) {
            return null;
        }
        if (this.state.store.currentUsernameTaken || this.state.username.length > UserStatics.modalFormMaxLength()) {
            return 'error';
        }
        return 'success';
    },

    getPasswordFormValidationState() {
        if (this.state.password.length == 0 && this.state.passwordValidation.length == 0) {
            return null;
        }
        if (this.state.password.length > UserStatics.modalFormMaxLength() || this.state.passwordValidation !== this.state.password) {
            return 'error';
        }
        return 'success';
    },

    renderForm() {
        let usernameForm = '';
        let optionalPasswordLabel = '';

        switch (this.state.store.modalState) {
            case UserStatics.stateNew():
                usernameForm = (
                    <div>
                        <div style={{display: "flex"}} className="pbs">
                            <ControlLabel className="mutedBold"><FormattedMessage id="userEditModal.form.label.username"/></ControlLabel>
                            <div style={{flexGrow: 1}}></div>
                            {this.state.store.currentUsernameTaken && this.state.username && <FormattedMessage id="userEditModal.form.label.usernameTaken"/>}
                        </div>
                        <FormControl
                            onChange={(e) => {
                                this.setState({username: e.target.value});
                                UserActions.checkUsernameTaken(e.target.value);
                            }}
                            type="text"
                            value={this.state.username}
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
                        <span className="mls">{this.state.username}</span>
                    </div>
                );
                break;
        }

        let options = []
        for (let i of [UserStatics.authorityPm(), UserStatics.authorityTranslator(), UserStatics.authorityAdmin(), UserStatics.authorityUser()]) {
            options.push(<option key={i} value={i}>{this.props.intl.formatMessage({id: roleToIntlKey(i)})}</option>)
        }

        return (
            <Form>
                <FormGroup validationState={this.getUsernameFormValidationState()} controlId="username Form">{usernameForm}</FormGroup>
                <FormGroup controlId="name Form">
                    <ControlLabel className="mutedBold pbs"><FormattedMessage id="userEditModal.form.label.givenName"/></ControlLabel>
                    <FormControl
                        onChange={(e) => this.setState({givenName: e.target.value})}
                        type="text"
                        value={this.state.givenName || ''}
                        placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.placeholder.givenName" })}
                    />

                    <ControlLabel className="mutedBold mtm pbs"><FormattedMessage id="userEditModal.form.label.surname"/></ControlLabel>
                    <FormControl
                        onChange={(e) => this.setState({surname: e.target.value})}
                        type="text"
                        value={this.state.surname || ''}
                        placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.placeholder.surname" })}
                    />

                    <ControlLabel className="mutedBold mtm pbs"><FormattedMessage id="userEditModal.form.label.commonName"/></ControlLabel>
                    <FormControl
                        onChange={(e) => this.setState({commonName: e.target.value})}
                        type="text"
                        value={this.state.commonName || ''}
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
                        onChange={(e) => this.setState({password: e.target.value})}
                        type="password"
                        value={this.state.password || ''}
                        placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.placeholder.password" })}
                    />
                    <FormControl
                        id="password validation input"
                        className="mtm"
                        onChange={(e) => this.setState({passwordValidation: e.target.value})}
                        type="password"
                        value={this.state.passwordValidation || ''}
                        placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.placeholder.passwordValidation" })}
                    />
                </FormGroup>
                <hr style={{marginBottom: "0px"}}/>
                <FormGroup>
                    <ControlLabel className="mutedBold mtm pbs"><FormattedMessage id="userEditModal.form.label.authority"/></ControlLabel>

                    <div style={{display: "flex", gap: "10px", alignItems: "center"}}>
                        <FormControl
                            onChange={(e) => this.setState({role: e.target.value})}
                            componentClass="select"
                            placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.select.placeholder" })}
                            style={{flexGrow: 1}}
                            value={this.state.role}
                        >
                            {options}
                        </FormControl>
                        <Glyphicon
                            className="clickable"
                            style={{fontSize: "20px"}}
                            glyph="glyphicon glyphicon-info-sign"
                            onClick={()=> this.setState({ infoAlert: !this.state.infoAlert })}
                        />
                    </div>
                    <Collapse in={this.state.infoAlert} className="mtm">
                        <div>
                            <Alert bsStyle="info">
                                <p className="text-muted brand-font-weight"><FormattedMessage id="users.userInformationAlert"/></p>
                            </Alert>
                        </div>
                    </Collapse>
                </FormGroup>
            </Form>
        );
    },

    isUsernameTaken() {
        if (this.state.store.modalState !== UserStatics.stateNew())
            return false;
        else
            return !(this.state.store.currentUsernameChecked && !this.state.store.currentUsernameTaken);
    },

    arePasswordStatesWrong() {
        let passwordValue = this.state.password;
        let passwordValidationValue = this.state.passwordValidation;
        let isPasswordLengthExceeded = passwordValue.length > UserStatics.modalFormMaxLength() || passwordValidationValue.length > UserStatics.modalFormMaxLength();
        let isPasswordEmpty = passwordValidationValue.length <= 0 || passwordValue.length <= 0;

        // entering a password on State "EDIT" is optional
        if (this.state.store.modalState === UserStatics.stateEdit()) {
            return passwordValue !== passwordValidationValue || isPasswordLengthExceeded;
        } else if (this.state.store.modalState === UserStatics.stateNew()) {
            return passwordValue !== passwordValidationValue || isPasswordEmpty || isPasswordLengthExceeded;
        } else
            return false;
    },

    valueCheckerPassed() {
        let usernameAlreadyTaken = this.isUsernameTaken();
        let passwordCheckFailed = this.arePasswordStatesWrong();
        let usernameLength = this.state.username.length;

        return !(usernameAlreadyTaken || !(this.state.username && this.state.username.trim()) || passwordCheckFailed || usernameLength > UserStatics.modalFormMaxLength());
    },

    onUserEditModalSaveClicked() {
        if (this.valueCheckerPassed()) {
            let savedUser = {
                "username": (this.state.username || '').trim(),
                "givenName": (this.state.givenName || '').trim(),
                "surname": (this.state.surname || '').trim(),
                "commonName": (this.state.commonName || '').trim(),
                "password": this.state.password,
                "authorities": [{
                    "authority": this.state.role
                }],
            };

            UserActions.closeUserModal();

            switch (this.state.store.modalState) {
                case UserStatics.stateEdit():
                    UserActions.saveEditRequest({user: savedUser, id: this.state.store.currentUser.id});
                    break;
                case UserStatics.stateNew():
                    UserActions.saveNewRequest(savedUser);
                    break;
            }
        } else {
            this.setState({
                alertCollapse: true
            });
        }
    },

    modalTitle() {
        let modalStateTitle = null;
        switch (this.state.store.modalState) {
            case UserStatics.stateEdit() :
                modalStateTitle = <FormattedMessage id="userEditModal.edit.title"/>;
                break;
            case UserStatics.stateNew() :
                modalStateTitle = <FormattedMessage id="userEditModal.new.title"/>;
                break;
            default :
                modalStateTitle = this.state.store.modalState;
        }

        return (
            <span className="mutedBold">{modalStateTitle}</span>
        );
    },

    renderValueAlert() {
        return (
            <Collapse in={this.state.alertCollapse}>
                <div>
                    <Alert bsStyle="danger">
                        <p className="text-center text-muted"><FormattedMessage id="userEditModal.alertMessage"/></p>
                    </Alert>
                </div>
            </Collapse>
        );
    },

    render() {
        return (
            <div>
                <Modal bsSize="large" show={this.state.store.modalState != UserStatics.stateHidden()} onHide={UserActions.closeUserModal}>
                    <Modal.Header closeButton>
                        <Modal.Title>{this.modalTitle()}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        {this.renderValueAlert()}
                        {this.renderForm()}
                    </Modal.Body>
                    <Modal.Footer>
                        <div className="text-center mbm">
                            <Button bsStyle="primary" onClick={this.onUserEditModalSaveClicked} disabled={!this.valueCheckerPassed()}>
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
});

export default injectIntl(UserModal);
