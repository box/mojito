import React from "react";
import createReactClass from "create-react-class";
import PropTypes from "prop-types";
import { Button, Modal, Form } from "react-bootstrap";
import CreateJobRepositoryDropDown from "./CreateJobRepositoryDropDown";
import JobActions from "../../actions/jobs/JobActions";

const CreateJobModal = createReactClass({
    displayName: "CreateJobModal",
    propTypes: {
        show: PropTypes.bool.isRequired,
        onClose: PropTypes.func.isRequired,
    },

    getInitialState() {
        return {
            selectedRepository: null,
        };
    },

    handleRepositorySelect(selectedRepository) {
        this.setState({ selectedRepository });
    },

    handleSubmit() {
        JobActions.createJob(this.state.selectedRepository);
        this.setState({ selectedRepository: null });
        this.props.onClose();
    },

    render() {
        return (
            <Modal show={this.props.show} onHide={this.props.onClose}>
                <Form
                    onSubmit={e => {
                        e.preventDefault();
                        this.handleSubmit();
                    }}
                >
                    <Modal.Header closeButton>
                        <Modal.Title>Create a Scheduled Job</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <CreateJobRepositoryDropDown
                            selected={this.state.selectedRepository}
                            onSelect={this.handleRepositorySelect}
                        />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={this.props.onClose}>
                            Close
                        </Button>
                        <Button
                            variant="primary"
                            type="submit"
                            disabled={!this.state.selectedRepository}
                        >
                            Submit
                        </Button>
                    </Modal.Footer>
                </Form>
            </Modal>
        );
    }
});

export default CreateJobModal;