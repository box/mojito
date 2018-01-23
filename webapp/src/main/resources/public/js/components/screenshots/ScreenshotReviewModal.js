import $ from "jquery";
import _ from "lodash";
import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from 'react-intl';
import {Button, ButtonGroup, ButtonToolbar, FormControl, Modal} from "react-bootstrap";
import keymirror from "keymirror";
import StatusCommon, {StatusCommonTypes} from "./StatusCommon";

class ScreenshotReviewModal extends React.Component {
    
    static propTypes = {
        "comment" : PropTypes.string.isRequired,
        "status": PropTypes.oneOf([
            StatusCommonTypes.ACCEPTED,
            StatusCommonTypes.NEEDS_REVIEW,
            StatusCommonTypes.REJECTED]).isRequired,
        "show" : PropTypes.bool.isRequired,
        "saveDisabled" : PropTypes.bool.isRequired,
        "onSave" : PropTypes.func.isRequired,
        "onCancel" : PropTypes.func.isRequired,
        "onCommentChanged" : PropTypes.func.isRequired,
        "onStatusChanged" : PropTypes.func.isRequired,
    }

    /**
     * @returns {JSX} The JSX for the reject button with class active set according to the current component state
     */
    getButton(status) {
        return (
            <Button active={this.props.status === status}
                    onClick={() => {this.props.onStatusChanged(status)}}>
                {StatusCommon.getScreenshotStatusIntl(this.props.intl, status)}
            </Button>
        );
    }

    /**
     * @return {JSX}
     */
    render() {
        return (
            <Modal show={this.props.show} onHide={this.props.onCancel}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="screenshots.reviewModal.title"/></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <FormControl
                        defaultValue={this.props.comment}
                        onChange={(e) => this.props.onCommentChanged(e.target.value)}
                        componentClass="textarea"
                        label={this.props.intl.formatMessage({ id: "screenshots.reviewModal.commentLabel" })}
                        placeholder={this.props.intl.formatMessage({ id: "screenshots.reviewModal.commentPlaceholder" })}/>
                    <ButtonToolbar className="mts">
                        <ButtonGroup ref="optionsGroup">
                            {this.getButton(StatusCommonTypes.ACCEPTED)}
                            {this.getButton(StatusCommonTypes.NEEDS_REVIEW)}
                            {this.getButton(StatusCommonTypes.REJECTED)}
                        </ButtonGroup>
                    </ButtonToolbar>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="primary" onClick={this.props.onSave}
                            disabled={this.props.saveDisabled}>
                        <FormattedMessage id="label.save"/>
                    </Button>
                    <Button onClick={this.props.onCancel}>
                        <FormattedMessage id="label.cancel"/>
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
};

export default injectIntl(ScreenshotReviewModal);
