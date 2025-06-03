import React from "react";
import createReactClass from "create-react-class";
import { Button } from "react-bootstrap";
import CreateJobModal from "./CreateJobModal";

const CreateJobButton = createReactClass({
    displayName: "CreateJobButton",
    getInitialState() {
        return {
            modalOpen: false
        }
    },
    openModal() {
        this.setState({ modalOpen: true });
    },
    closeModal() {
        this.setState({ modalOpen: false });
    },
    render() {
        return (
            <div>
                <Button bsStyle="primary" onClick={this.openModal}>
                    Create Job
                </Button>
                <CreateJobModal show={this.state.modalOpen} onClose={this.closeModal} />
            </div>
        );
    }
});

export default CreateJobButton;