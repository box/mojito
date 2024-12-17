import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, Modal} from "react-bootstrap";

class UserDeleteModal extends React.Component {
    render() {
        return (
            <div>
                <Modal bsSize="large" show={this.props.showDeleteUserModal} onHide={this.props.onClose}>
                    <Modal.Header closeButton>
                        <Modal.Title><FormattedMessage id="userDeleteModal.title" values={{username: this.props.currentUser.username}}/></Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <FormattedMessage id="userDeleteModal.query" values={{username: this.props.currentUser.username}}/>
                    </Modal.Body>
                    <Modal.Footer>
                        <div className="text-center mbm">
                            <Button bsStyle="primary" onClick={() => this.props.onDeleteUserConfirmed(this.props.currentUser.id)}>
                                <FormattedMessage id="label.delete"/>
                            </Button>
                            <Button onClick={this.props.onClose}>
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
