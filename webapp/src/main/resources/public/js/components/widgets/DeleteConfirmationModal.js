import React from "react";
import {FormattedMessage} from "react-intl";
import {Button, Modal} from "react-bootstrap";

let DeleteConfirmationModal = React.createClass({

    propTypes: {
        "showModal": React.PropTypes.bool.isRequired,
        "modalBodyMessage": React.PropTypes.string.isRequired,
        "onDeleteCancelledCallback": React.PropTypes.func.isRequired,
        "onDeleteClickedCallback": React.PropTypes.func.isRequired
    },

    getDefaultProps() {
        return {
            "showModal": false
        };
    },

    render() {
        return (
            <Modal show={this.props.showModal} onHide={this.props.onDeleteCancelledCallback}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="textUnit.deletemodal.title"/></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <FormattedMessage id={this.props.modalBodyMessage}/>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="danger" onClick={this.props.onDeleteClickedCallback}>
                        <FormattedMessage id="label.delete"/>
                    </Button>
                    <Button onClick={this.props.onDeleteCancelledCallback}>
                        <FormattedMessage id="label.cancel"/>
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
});

export default DeleteConfirmationModal;
