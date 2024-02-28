import React from "react";
import {FormattedMessage, intlShape, injectIntl} from "react-intl";
import {Button, Modal} from "react-bootstrap";

class UserErrorModal extends React.Component {
    render() {
        return (
            <div>
                <Modal bsSize="large" show={this.props.lastErrorKey != null} onHide={this.props.onClose}>
                    <Modal.Header closeButton>
                        <Modal.Title><FormattedMessage id="userErrorModal.title"/></Modal.Title>
                    </Modal.Header>
                    <Modal.Body className="text-center">
                        {this.props.lastErrorKey && <FormattedMessage id={this.props.lastErrorKey}/>}
                        {this.props.lastErrorKey && this.props.lastErrorCode === 403 &&
                            <div>
                                <br />
                                <br />
                                <FormattedMessage id="userErrorModal.forbidden"/>
                            </div>
                        }
                    </Modal.Body>
                    <Modal.Footer>
                        <div className="text-center mbm">
                            <Button onClick={this.props.onClose}>
                                <FormattedMessage id="label.cancel"/>
                            </Button>
                        </div>
                    </Modal.Footer>
                </Modal>
            </div>
        );
    }
};

export default injectIntl(UserErrorModal);
