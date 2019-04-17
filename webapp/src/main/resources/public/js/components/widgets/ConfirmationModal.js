/**
 * Generic confirmation modal. You must supply the body message and the confirm and cancel
 * button callbacks, as well as the confirm and cancel button labels.
 */
import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage} from "react-intl";
import {Button, Modal} from "react-bootstrap";

class ConfirmationModal extends React.Component {
    static propTypes = {
        "showModal": PropTypes.bool.isRequired,
        "modalTitleMessage": PropTypes.string.isRequired,
        "modalBodyMessage": PropTypes.string.isRequired,
        "onCancelledCallback": PropTypes.func.isRequired,
        "onConfirmedCallback": PropTypes.func.isRequired,
        "confirmButtonLabel": PropTypes.string.isRequired,
        "cancelButtonLabel": PropTypes.string.isRequired
    };

    static defaultProps = {
        "showModal": false
    };

    render() {
        return (
            <Modal show={this.props.showModal} onHide={this.props.onCancelledCallback}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id={this.props.modalTitleMessage}/></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <FormattedMessage id={this.props.modalBodyMessage}/>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="danger" onClick={this.props.onConfirmedCallback}>
                        <FormattedMessage id={this.props.confirmButtonLabel}/>
                    </Button>
                    <Button onClick={this.props.onCancelledCallback}>
                        <FormattedMessage id={this.props.cancelButtonLabel}/>
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

export default ConfirmationModal;
