import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, ButtonGroup, ButtonToolbar, FormControl, Modal} from "react-bootstrap";
import TextUnit from "../../sdk/TextUnit";

class TextUnitsreviewModal extends React.Component {
    static propTypes() {
        return {
            "isShowModal": PropTypes.bool.isRequired
        };
    }

    static defaultProps = {
        "isShowModal": false
    };

    constructor(props, context) {
        super(props, context);
        this.REVIEW = "review";
        this.REJECT = "reject";
        this.MACHINE_TRANSLATED = "machine translated";
        this.MT_REVIEW = "mt review needed";
        this.ACCEPT = "accept";
        this.TRANSLATE = "translate"
        this.OVERRIDDEN = "overridden";

        this.state = {
            "currentReviewState": this.getInitialReviewStateOfTextUnits(),
            "comment": this.getInitialTargetCommentOfTextUnits()
        };
    }

    isMTState = () => {
        return this.state.currentReviewState === this.MACHINE_TRANSLATED || this.state.currentReviewState === this.MT_REVIEW;
    }

    /**
     * Sets the state of the component to the button that was clicked upon.
     * @param {string} reviewState
     */
    optionClicked = (reviewState) => {
        this.setState({
            "currentReviewState": reviewState
        });
    };

    /**
     * Creates the data object containing comments and chosen action option and calls the
     * parent action handler to perform the action on the textunit.
     */
    onReviewModalSaveClicked = () => {
        let modalData = {
            "comment": this.state.comment,
            "textUnitAction": this.state.currentReviewState
        };
        this.props.onReviewModalSaveClicked(modalData);
    };

    /**
     * Closes the modal and calls the parent action handler to mark the modal as closed
     */
    closeModal = () => {
        this.setState({
            isShowModal: false
        });
        this.props.onCloseModal();
    };

    /**
     * @returns {JSX} The JSX for the reject button with class active set according to the current component state
     */
    getRejectButton = () => {
        return (
            <Button active={this.state.currentReviewState === this.REJECT}
                    onClick={this.optionClicked.bind(this, this.REJECT)}
                    disabled={this.isMTState()}>

                <FormattedMessage id="textUnit.reviewModal.rejected"/>
            </Button>
        );
    };

    /**
     * @returns {JSX} The JSX for the MT review button with class active set according to the current component state
     */
    getMTReviewNeededButton = () => {
        return (
            <Button active={this.state.currentReviewState === this.MT_REVIEW}
                    onClick={this.optionClicked.bind(this, this.MT_REVIEW)}
                    disabled={true}>

                <FormattedMessage id="textUnit.reviewModal.mtReview"/>
            </Button>
        );
    };

    /**
     * @returns {JSX} The JSX for the Machine Translated button with class active set according to the current component state
     */
    getMTButton = () => {
        return (
            <Button active={this.state.currentReviewState === this.MACHINE_TRANSLATED}
                    onClick={this.optionClicked.bind(this, this.MACHINE_TRANSLATED)}
                    disabled={true}>

                <FormattedMessage id="textUnit.reviewModal.mt"/>
            </Button>
        );
    };

    /**
     * @returns {JSX} The JSX for the review button with class active set according to the current component state
     */
    getReviewButton = () => {
        return (
            <Button active={this.state.currentReviewState === this.REVIEW}
                    onClick={this.optionClicked.bind(this, this.REVIEW)}
                    disabled={this.isMTState()}>

                <FormattedMessage id="textUnit.reviewModal.needsReview"/>
            </Button>
        );
    };

    /**
     * @returns {JSX} The JSX for the accept button with class active set according to the current component state
     */
    getAcceptButton = () => {
        return (
            <Button active={this.state.currentReviewState === this.ACCEPT}
                    onClick={this.optionClicked.bind(this, this.ACCEPT)}
                    disabled={this.isMTState()}>

                <FormattedMessage id="textUnit.reviewModal.accepted"/>
            </Button>
        );
    };

    getOverriddenButton = () => {
        return (
            <Button active={this.state.currentReviewState === this.OVERRIDDEN}
                    onClick={this.optionClicked.bind(this, this.OVERRIDDEN)}
                    disabled={this.isMTState()}>
                <FormattedMessage id="textUnit.reviewModal.overridden"/>
            </Button>
        );
    };

    /**
     * @returns {JSX} The JSX for the translate button with class active set according to the current component state
     */
    getTranslateButton = () => {
        return (
            <Button active={this.state.currentReviewState === this.TRANSLATE}
                    onClick={this.optionClicked.bind(this, this.TRANSLATE)}
                    disabled={this.isMTState()}>

                <FormattedMessage id="textUnit.reviewModal.translationNeeded"/>
            </Button>
        );
    };

    /**
     * @returns {string} The current review state of the selected textunits (reject/review/accept)
     * if ALL textunits have the same state, empty string otherwise.
     */
    getInitialReviewStateOfTextUnits = () => {
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
    };

    /**
     * @returns {string} The target comment of the selected textunits if ALL textunits have
     * the same comment, empty string otherwise.
     */
    getInitialTargetCommentOfTextUnits = () => {
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
    };

    /**
     * @param {TextUnit} textUnit
     * @returns {string} The current review state of the textUnit passed in.
     */
    getReviewStateOfTextUnit = (textUnit) => {
        let currentReviewState = "";
        if (typeof textUnit !== "undefined") {
            currentReviewState = this.ACCEPT;
            if (textUnit.getStatus() === TextUnit.STATUS.MACHINE_TRANSLATED) {
                currentReviewState = this.MACHINE_TRANSLATED;
            }
            else if (textUnit.getStatus() === TextUnit.STATUS.MT_REVIEW_NEEDED) {
                currentReviewState = this.MT_REVIEW;
            }
            else if (!textUnit.isIncludedInLocalizedFile()) {
                currentReviewState = this.REJECT;
            } else if (textUnit.getStatus() === TextUnit.STATUS.REVIEW_NEEDED) {
                currentReviewState = this.REVIEW;
            } else if (textUnit.getStatus() === TextUnit.STATUS.TRANSLATION_NEEDED) {
                currentReviewState = this.TRANSLATE;
            } else if (textUnit.getStatus() === TextUnit.STATUS.OVERRIDDEN) {
                currentReviewState = this.OVERRIDDEN;
            }

        }
        return currentReviewState;
    };

    /**
     * @param {TextUnit} textUnit
     * @returns {string} The target comment of the textunit passed in.
     */
    getTargetCommentOfTextUnit = (textUnit) => {
        let targetComment = "";
        if (typeof textUnit !== "undefined") {
            targetComment = textUnit.getTargetComment();
        }
        return targetComment;
    };

    /**
     * @param {Event} event
     */
    commentTextAreaOnChange = (event) => {
        this.setState({"comment": event.target.value});
    };

    render() {
        return (
            <Modal show={this.props.isShowModal} onHide={this.closeModal} onKeyUp={(e) => {
                e.stopPropagation()
            }}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="textUnit.reviewModal.title"/></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <FormControl
                        defaultValue={this.state.comment ? this.state.comment : ""}
                        onChange={this.commentTextAreaOnChange}
                        componentClass="textarea"
                        label={this.props.intl.formatMessage({id: "textUnit.reviewModal.commentLabel"})}
                        placeholder={this.props.intl.formatMessage({id: "textUnit.reviewModal.commentPlaceholder"})}
                        onClick={(e) => {
                            e.stopPropagation();
                        }}
                    />
                    <ButtonToolbar className="mts">
                        <ButtonGroup ref="optionsGroup">
                            {this.getRejectButton()}
                            {this.getTranslateButton()}
                            {this.getMTButton()}
                            {this.getMTReviewNeededButton()}
                            {this.getReviewButton()}
                            {this.getAcceptButton()}
                            {this.getOverriddenButton()}
                        </ButtonGroup>
                    </ButtonToolbar>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="primary" onClick={this.onReviewModalSaveClicked}
                            disabled={this.state.currentReviewState === "" || this.isMTState()} >
                        <FormattedMessage id="label.save"/>
                    </Button>
                    <Button onClick={this.closeModal}>
                        <FormattedMessage id="label.cancel"/>
                    </Button>
                </Modal.Footer>
            </Modal>
        )
            ;
    }
}

export default injectIntl(TextUnitsreviewModal);
