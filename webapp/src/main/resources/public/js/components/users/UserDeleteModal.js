import React from "react";
import {FormattedMessage, intlShape, injectIntl} from "react-intl";
import {Button, Modal} from "react-bootstrap";
import FluxyMixin from "alt-mixins/FluxyMixin";
import UserStore from "../../stores/users/UserStore";
import UserActions from "../../actions/users/UserActions";


let createClass = require('create-react-class');

let UserDeleteModal = createClass({

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
        UserActions.closeUserModal();
        UserActions.deleteRequest(this.state.store.currentUser.id);
        UserActions.reloadCurrentPage();
    },

    render() {
        return (
            <div>
                <Modal bsSize="large" show={this.state.store.showDeleteUserModal} onHide={UserActions.closeUserModal}>
                    <Modal.Header closeButton>
                        <Modal.Title><FormattedMessage id="userDeleteModal.title" values={{username: this.state.store.currentUser.username}}/></Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <FormattedMessage id="userDeleteModal.query" values={{username: this.state.store.currentUser.username}}/>
                    </Modal.Body>
                    <Modal.Footer>
                        <div className="text-center mbm">
                            <Button bsStyle="primary" onClick={this.onDeleteUserConfirmed}>
                                <FormattedMessage id="label.delete"/>
                            </Button>
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

export default injectIntl(UserDeleteModal);
