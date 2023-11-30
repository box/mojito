import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Alert, Button, Modal} from "react-bootstrap";
import ShareSearchParamsModalStore from "../../stores/workbench/ShareSearchParamsModalStore";

class ShareSearchParamsModal extends React.Component {
    static propTypes = {
        "show": PropTypes.bool.isRequired,
        "onCancel": PropTypes.func.isRequired,
        "onCopy": PropTypes.func.isRequired,
        "isLoadingParams": PropTypes.bool.isRequired,
        "url": PropTypes.string,
        "errorMessage": PropTypes.string,
    }

    render() {
        return (
            <Modal show={this.props.show} onHide={this.props.onCancel}>
                <Modal.Header closeButton>
                    <Modal.Title>
                        <FormattedMessage id="workbench.shareSearchParamsModal.title"/>
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {this.props.isLoadingParams ? (<span className="glyphicon glyphicon-refresh spinning"/>) : ""}

                    <a href={this.props.url}>{this.props.url}</a>
                    {
                        this.props.errorType != null &&
                        <Alert bsStyle="danger">{this.getErrorMessage()}</Alert>
                    }
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="primary" onClick={() => this.props.onCopy(this.props.url)}>
                        <FormattedMessage id="workbench.shareSearchParamsModal.copy"/>
                    </Button>
                    <Button onClick={this.props.onCancel}>
                        <FormattedMessage id="label.cancel"/>
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }

    getErrorMessage() {
        switch (this.props.errorType) {
            case ShareSearchParamsModalStore.ERROR_TYPES.GET_SEARCH_PARAMS:
                return this.props.intl.formatMessage({id: "workbench.shareSearchParamsModal.errors.get"});
            case ShareSearchParamsModalStore.ERROR_TYPES.SAVE_SEARCH_PARAMS:
                return this.props.intl.formatMessage({id: "workbench.shareSearchParamsModal.errors.save"});
            case ShareSearchParamsModalStore.ERROR_TYPES.COPY_TO_CLIPBOARD:
                return this.props.intl.formatMessage({id: "workbench.shareSearchParamsModal.errors.copyToClipboard"});
        }
    }
}

export default injectIntl(ShareSearchParamsModal);
