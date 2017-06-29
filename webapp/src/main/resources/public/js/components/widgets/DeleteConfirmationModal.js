import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage} from "react-intl";
import {Button, Modal} from "react-bootstrap";

let DeleteConfirmationModal = React.createClass({

    propTypes: {
        "showModal": PropTypes.bool.isRequired,
        "modalBodyMessage": PropTypes.string.isRequired,
        "onDeleteCancelledCallback": PropTypes.func.isRequired,
        "onDeleteClickedCallback": PropTypes.func.isRequired
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
