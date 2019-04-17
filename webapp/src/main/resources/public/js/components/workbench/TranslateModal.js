import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage} from "react-intl";
import {Button, Modal, ButtonToolbar, ButtonGroup} from "react-bootstrap";

class TranslateModal extends React.Component {
    static propTypes = {
        "showModal": PropTypes.bool.isRequired,
        "onCancel": PropTypes.func.isRequired,
        "onSave": PropTypes.func.isRequired
    };

    static defaultProps = {
        "showModal": false
    };

    state = {
        "translate": true
    };

    render() {
        return (
            <Modal show={this.props.showModal} onHide={this.props.onCancel}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="textUnit.TranslateModal.title"/></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div>
                        <FormattedMessage id="textUnit.TranslateModal.body"/>
                    </div>

                    <ButtonToolbar className="mts">
                        <ButtonGroup ref="optionsGroup">
                            <Button active={this.state.translate == true}
                                    onClick={() => this.setState({"translate": true})}>
                                <FormattedMessage id="textUnit.TranslateModal.translate"/>
                            </Button>
                            <Button active={this.state.translate == false}
                                    onClick={() => this.setState({"translate": false})}>
                                <FormattedMessage id="textUnit.TranslateModal.doNotTranslate"/>
                            </Button>
                        </ButtonGroup>
                    </ButtonToolbar>
                </Modal.Body>

                <Modal.Footer>
                    <Button bsStyle="primary" onClick={() => this.props.onSave(this.state.translate)}>
                        <FormattedMessage id="label.save"/>
                    </Button>

                    <Button onClick={this.props.onCancel}>
                        <FormattedMessage id="label.cancel"/>
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

export default TranslateModal;
