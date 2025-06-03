import React from "react";
import createReactClass from "create-react-class";
import PropTypes from "prop-types";
import { Button, Modal } from "react-bootstrap";
import CreateJobRepositoryDropDown from "./CreateJobRepositoryDropDown";

const CreateJobModal = createReactClass({
    displayName: "CreateJobModal",
    propTypes: {
        show: PropTypes.bool.isRequired,
        onClose: PropTypes.func.isRequired
    },

    render() {
        return (
            <Modal show={this.props.show} onHide={this.props.onClose}>
                <Modal.Header closeButton>
                    <Modal.Title>Create a Scheduled Job</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <CreateJobRepositoryDropDown/>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.props.onClose}>
                        Close
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
});

export default CreateJobModal;