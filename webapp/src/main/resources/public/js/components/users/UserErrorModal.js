import React from "react";
import {FormattedMessage, intlShape, injectIntl} from "react-intl";
import {
    Popover, Button, FormControl, Modal, Form, FormGroup, ControlLabel, Alert, Collapse, Glyphicon, OverlayTrigger, Checkbox,
    Badge, Fade
} from "react-bootstrap";
import FluxyMixin from "alt-mixins/FluxyMixin";
import UserStore from "../../stores/users/UserStore";
import UserActions from "../../actions/users/UserActions";


let createClass = require('create-react-class');

let UserErrorModal = createClass({

    mixins: [FluxyMixin],

    propTypes: {
        intl: intlShape.isRequired,
    },

    statics: {
        storeListeners: {
            "onUserStoreChanged": UserStore,
        }
    },

    onUserStoreChanged() {
        this.setState({
            store: UserStore.getState(),
        });
    },

    getInitialState() {
        const store = UserStore.getState();
        return {
            store: store,
        };
    },

    onDeleteUserConfirmed() {
        UserActions.deleteRequest(this.state.store.currentUser.id);
        UserActions.closeUserModal();
    },

    render() {
        return (
            <div>
                <Modal bsSize="large" show={this.state.store.lastErrorKey != null} onHide={UserActions.closeUserModal}>
                    <Modal.Header closeButton>
                        <Modal.Title><FormattedMessage id="userErrorModal.title"/></Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        {this.state.store.lastErrorKey && <FormattedMessage id={this.state.store.lastErrorKey}/>}
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
});

export default injectIntl(UserErrorModal);
