import React from "react";
import {Button, FormControl, Modal, FormGroup, ControlLabel, Alert, Collapse} from "react-bootstrap";
import {FormattedMessage, injectIntl} from "react-intl";
import UserStatics from "../../utils/UserStatics";
import UserClient from "../../sdk/UserClient";
import createReactClass from "create-react-class";

let ChangePasswordModal = createReactClass({

    getInitialState() {
        return {
            currPasswordValue: '',
            newPasswordValue: '',
            newPasswordValidationValue: '',
            showAlert: false,
        };
    },

    isValidPassword() {
        let passwordState = this.state.newPasswordValue;
        let passwordValidationState = this.state.newPasswordValidationValue;

        let isPasswordEmpty = passwordState.length <= 0 || passwordValidationState.length <= 0;
        let isPasswordLengthExceeded = passwordState.length > UserStatics.modalFormMaxLength() || passwordValidationState.length > UserStatics.modalFormMaxLength();

        return !isPasswordEmpty && !isPasswordLengthExceeded && passwordState === passwordValidationState;
    },

    onSaveClicked() {
        UserClient
            .updatePassword(this.state.currPasswordValue, this.state.newPasswordValue)
            .then(() => {
                this.doClose();
            })
            .catch(() => {
                this.setState({showAlert: true});
            });
    },

    getAlert() {
        return (
            <Collapse in={this.state.showAlert}>
                <div>
                    <Alert bsStyle="danger">
                        <p className="text-center text-muted"><FormattedMessage id="userEditModal.alertMessage"/></p>
                    </Alert>
                </div>
            </Collapse>
        );
    },

    doClose() {
        this.setState(this.getInitialState());
        this.props.onClose();
    },

    render() {
        return (
            <Modal show={this.props.show} onHide={this.doClose}>
                <Modal.Header>
                    <Modal.Title className="text-center">
                        <span className="mutedBold"><FormattedMessage id="header.change-pw"/></span></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {this.getAlert()}
                    <FormGroup validationState={this.state.currPasswordValue ? "success" : "error"}>
                        <ControlLabel className="mutedBold pbs"><FormattedMessage id="userEditModal.form.label.currentPassword"/></ControlLabel>
                        <FormControl id="current password input"
                                     onChange={(e) => this.setState({currPasswordValue: e.target.value})}
                                     type="password" value={this.state.currPasswordValue}
                                     placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.placeholder.currentPassword" })}/>
                    </FormGroup>

                    <hr />

                    <FormGroup validationState={this.isValidPassword() ? "success" : "error"}>
                        <ControlLabel className="mutedBold pbs"><FormattedMessage id="userEditModal.form.label.newPassword"/></ControlLabel>
                        <FormControl id="password input"
                                     onChange={(e) => this.setState({newPasswordValue: e.target.value})}
                                     type="password" value={this.state.newPasswordValue}
                                     placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.placeholder.password" })}/>
                        <FormControl id="password validation input"
                                     className="mtm"
                                     onChange={(e) => this.setState({newPasswordValidationValue: e.target.value})}
                                     type="password" value={this.state.newPasswordValidationValue}
                                     placeholder={this.props.intl.formatMessage({ id: "userEditModal.form.placeholder.passwordValidation" })}/>
                    </FormGroup>
                </Modal.Body>
                <Modal.Footer>
                    <div className="text-center">
                        <Button bsStyle="primary" onClick={this.onSaveClicked} disabled={!this.isValidPassword() || !this.state.currPasswordValue}>
                            <FormattedMessage id="label.save"/>
                        </Button>
                        <Button onClick={this.doClose}>
                            <FormattedMessage id="label.cancel"/>
                        </Button>
                    </div>
                </Modal.Footer>
            </Modal>
        );
    }
});

export default injectIntl(ChangePasswordModal);
