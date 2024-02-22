import React from "react";
import {FormattedMessage, intlShape, injectIntl} from "react-intl";
import {Button, Modal} from "react-bootstrap";
import UserActions from "../../actions/users/UserActions";

class UserErrorModal extends React.Component {
    render() {
        return (
            <div>
                <Modal bsSize="large" show={this.props.lastErrorKey != null} onHide={UserActions.closeUserModal}>
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
                            <Button onClick={UserActions.closeUserModal}>
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
