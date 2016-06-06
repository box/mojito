import React from "react";
import ReactIntl from 'react-intl';
import {Button, Modal} from "react-bootstrap";

let {IntlMixin} = ReactIntl;

let DeleteConfirmationModal = React.createClass({

    mixins: [IntlMixin],

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
                    <Modal.Title>{this.getIntlMessage("textUnit.deletemodal.title")}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {this.props.modalBodyMessage}
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="danger" onClick={this.props.onDeleteClickedCallback}>
                        {this.getIntlMessage("label.delete")}
                    </Button>
                    <Button onClick={this.props.onDeleteCancelledCallback}>
                        {this.getIntlMessage("label.cancel")}
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
});

export default DeleteConfirmationModal;
