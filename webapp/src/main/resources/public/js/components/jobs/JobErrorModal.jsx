import React from "react";
import {Alert, Modal, Button} from "react-bootstrap";
import PropTypes from "prop-types";

class JobErrorModal extends React.Component {
    static propTypes = {
        showModal: PropTypes.bool.isRequired,
        errorMessage: PropTypes.string.isRequired,
        onErrorModalClosed: PropTypes.func.isRequired,
        title: PropTypes.string
    };
    render() {
        return (
            <Modal show={this.props.showModal} onHide={this.props.onErrorModalClosed}>
                <Modal.Header closeButton>
                    <Modal.Title>{this.props.title || "An Error Occurred"}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Alert bsStyle="danger">
                        {this.props.errorMessage}
                    </Alert>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={this.props.onErrorModalClosed}>
                        Cancel
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

export default JobErrorModal;
