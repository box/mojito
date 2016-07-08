import $ from "jquery";
import _ from "lodash";
import React from "react";
import LinkedStateMixin from "react-addons-linked-state-mixin";
import {FormattedMessage, injectIntl} from 'react-intl';
import {Button, ButtonGroup, ButtonToolbar, FormControl, Modal} from "react-bootstrap";

import SearchResultsStore from "../../stores/workbench/SearchResultsStore";

import TextUnit from "../../sdk/TextUnit";

let TextUnitsreviewModal = React.createClass({

    mixins: [LinkedStateMixin],

    propTypes() {
        return {
            "isShowModal": React.PropTypes.bool.isRequired
        }
    },

    getDefaultProps() {
        return {
            "isShowModal": false
        };
    },

    /**
     * @returns {{
     *      currentReviewState: {string} The current review state of the selected textunits in case of bulk operation or the textunit passed in as prop otherwise.
     *      comment: {string} The target comment to be prepopulated in textarea. In case of bulk operation, this is left blank.
     *  }}
     */
    getInitialState() {
        this.REVIEW = "review";
        this.REJECT = "reject";
        this.ACCEPT = "accept";
        this.TRANSLATE = "translate";
        return {
            "currentReviewState": this.getInitialReviewStateOfTextUnits(),
            "comment": this.getInitialTargetCommentOfTextUnits()
        }
    },

    /**
     * Sets the state of the component to the button that was clicked upon.
     * @param {SyntheticEvent} e The event object for the click event on text unit action options
     */
    optionClicked(e) {
        let $target = $(e.target);
        this.setState({
            "currentReviewState": $target.data("option")
        });
    },

    /**
     * Creates the data object containing comments and chosen action option and calls the
     * parent action handler to perform the action on the textunit.
     */
    onReviewModalSaveClicked() {
        let modalData = {
            "comment": this.state.comment,
            "textUnitAction": this.state.currentReviewState
        }
        this.props.onReviewModalSaveClicked(modalData);
    },

    /**
     * Closes the modal and calls the parent action handler to mark the modal as closed
     */
    closeModal() {
        this.setState({
            isShowModal: false
        });
        this.props.onCloseModal();
    },

    /**
     * @returns {JSX} The JSX for the reject button with class active set according to the current component state
     */
    getRejectButton() {
        return (
            <Button active={this.state.currentReviewState === this.REJECT}
                onClick={this.optionClicked} data-option={this.REJECT}>

                <FormattedMessage id="textUnit.reviewModal.rejected" />
            </Button>
        );
    },

    /**
     * @returns {JSX} The JSX for the review button with class active set according to the current component state
     */
    getReviewButton() {
        return (
            <Button active={this.state.currentReviewState === this.REVIEW}
                onClick={this.optionClicked} data-option={this.REVIEW}>

                <FormattedMessage id="textUnit.reviewModal.needsReview" />
            </Button>
        );
    },

    /**
     * @returns {JSX} The JSX for the accept button with class active set according to the current component state
     */
    getAcceptButton() {
        return (
            <Button active={this.state.currentReviewState === this.ACCEPT}
                onClick={this.optionClicked} data-option={this.ACCEPT}>

                <FormattedMessage id="textUnit.reviewModal.accepted" />
            </Button>
        );
    },

    /**
     * @returns {JSX} The JSX for the translate button with class active set according to the current component state
     */
    getTranslateButton() {
        return (
            <Button active={this.state.currentReviewState === this.TRANSLATE}
                onClick={this.optionClicked} data-option={this.TRANSLATE}>

                   <FormattedMessage id="textUnit.reviewModal.translationNeeded" />
            </Button>
        );
    },

    /**
     * @returns {string} The current review state of the selected textunits (reject/review/accept)
     * if ALL textunits have the same state, empty string otherwise.
     */
    getInitialReviewStateOfTextUnits() {
        let selectedTextUnits = this.props.textUnitsArray;
        let currentReviewState = "";
        if (selectedTextUnits.length > 0) {
            currentReviewState = this.getReviewStateOfTextUnit(selectedTextUnits[0]);
        }
        for (let index = 1; index < selectedTextUnits.length; index++) {
            let textUnitReviewState = this.getReviewStateOfTextUnit(selectedTextUnits[index]);
            if (textUnitReviewState !== currentReviewState) {
                currentReviewState = "";
                break;
            }
        }
        return currentReviewState;
    },

    /**
     * @returns {string} The target comment of the selected textunits if ALL textunits have
     * the same comment, empty string otherwise.
     */
    getInitialTargetCommentOfTextUnits() {
        let selectedTextUnits = this.props.textUnitsArray;
        let initialTargetComment = "";
        if (selectedTextUnits.length > 0) {
            initialTargetComment = this.getTargetCommentOfTextUnit(selectedTextUnits[0]);
        }
        for (let index = 1; index < selectedTextUnits.length; index++) {
            let textUnitTargetComment = this.getTargetCommentOfTextUnit(selectedTextUnits[index]);
            if (textUnitTargetComment !== initialTargetComment) {
                initialTargetComment = "";
                break;
            }
        }
        return initialTargetComment;
    },

    /**
     * @param {TextUnit} textUnit
     * @returns {string} The current review state of the textUnit passed in.
     */
    getReviewStateOfTextUnit(textUnit) {
        let currentReviewState = "";
        if (typeof textUnit !== "undefined") {
            currentReviewState = this.ACCEPT;
            if (!textUnit.isIncludedInLocalizedFile()) {
                currentReviewState = this.REJECT;
            } else if (textUnit.getStatus() === TextUnit.STATUS.REVIEW_NEEDED) {
                currentReviewState = this.REVIEW;
            } else if (textUnit.getStatus() === TextUnit.STATUS.TRANSLATION_NEEDED) {
                currentReviewState = this.TRANSLATE;
            }

        }
        return currentReviewState;
    },

    /**
     * @param {TextUnit} textUnit
     * @returns {string} The target comment of the textunit passed in.
     */
    getTargetCommentOfTextUnit(textUnit) {
        let targetComment = "";
        if (typeof textUnit !== "undefined") {
            targetComment = textUnit.getTargetComment();
        }
        return targetComment;
    },

    /**
     * @param {Event} event
     */
    commentTextAreaOnChange(event) {
        this.setState({ "comment": event.target.value});
    },

    render() {
        return (
            <Modal show={this.props.isShowModal} onHide={this.closeModal}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="textUnit.reviewModal.title" /></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <FormControl
                        defaultValue={this.state.comment ? this.state.comment : ""}
                        onChange={this.commentTextAreaOnChange}
                        componentClass="textarea"
                        label={this.props.intl.formatMessage({ id: "textUnit.reviewModal.commentLabel" })}
                        placeholder={this.props.intl.formatMessage({ id: "textUnit.reviewModal.commentPlaceholder" })} />
                    <ButtonToolbar>
                        <ButtonGroup ref="optionsGroup">
                            {this.getRejectButton()}
                            {this.getTranslateButton()}
                            {this.getReviewButton()}
                            {this.getAcceptButton()}
                        </ButtonGroup>
                    </ButtonToolbar>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="primary" onClick={this.onReviewModalSaveClicked}
                        disabled={this.state.currentReviewState === ""}>
                        <FormattedMessage id="label.save" />
                    </Button>
                    <Button onClick={this.closeModal}>
                        <FormattedMessage id="label.cancel" />
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
});

export default injectIntl(TextUnitsreviewModal);
