import React from "react";
import ReactIntl from "react-intl";
import {Alert, Button, Modal} from "react-bootstrap";

let {IntlMixin} = ReactIntl;

let ErrorModal = React.createClass({

    mixins: [IntlMixin],

    render() {
        return (
            <Modal show={this.props.showModal} onHide={this.props.onErrorModalClosed}>
                <Modal.Header closeButton>
                    <Modal.Title>{this.getIntlMessage("error.modal.title")}</Modal.Title>
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
});

export default ErrorModal;
