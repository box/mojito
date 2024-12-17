import React from "react";
import {FormattedMessage} from "react-intl";
import {Alert, Button, Modal} from "react-bootstrap";

class ErrorModal extends React.Component {
    render() {
        return (
            <Modal show={this.props.showModal} onHide={this.props.onErrorModalClosed}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="error.modal.title" /></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Alert bsStyle="danger">
                        {this.props.errorMessage}
                    </Alert>
                </Modal.Body>
                <Modal.Footer>
                </Modal.Footer>
            </Modal>
        );
    }
}

export default ErrorModal;
