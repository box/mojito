import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, Modal} from "react-bootstrap";
import UserActions from "../../actions/users/UserActions";

class UserDeleteModal extends React.Component {
    onDeleteUserConfirmed() {
        UserActions.closeUserModal();
        UserActions.deleteRequest(this.props.currentUser.id);
        UserActions.reloadCurrentPage();
    }

    render() {
        return (
            <div>
                <Modal bsSize="large" show={this.props.showDeleteUserModal} onHide={UserActions.closeUserModal}>
                    <Modal.Header closeButton>
                        <Modal.Title><FormattedMessage id="userDeleteModal.title" values={{username: this.props.currentUser.username}}/></Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <FormattedMessage id="userDeleteModal.query" values={{username: this.props.currentUser.username}}/>
                    </Modal.Body>
                    <Modal.Footer>
                        <div className="text-center mbm">
                            <Button bsStyle="primary" onClick={() => this.onDeleteUserConfirmed()}>
                                <FormattedMessage id="label.delete"/>
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
};

export default injectIntl(UserDeleteModal);
