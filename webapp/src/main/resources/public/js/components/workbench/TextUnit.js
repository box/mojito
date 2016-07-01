import $ from "jquery";
import keycode from "keycode";
import FluxyMixin from "alt/mixins/FluxyMixin";
import React from "react/addons";
import ReactIntl from "react-intl";
import Error from "../../utils/Error";
import SearchConstants from "../../utils/SearchConstants";
import SearchResultsStore from "../../stores/workbench/SearchResultsStore";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import RepositoryStore from "../../stores/RepositoryStore";
import TextUnitStore from "../../stores/workbench/TextUnitStore";
import TextUnitsReviewModal from "./TextUnitsReviewModal";
import TextUnitSDK from "../../sdk/TextUnit";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import Locales from "../../utils/Locales";
import {
    Alert,
    Grid,
    Row,
    Col,
    Panel,
    Button,
    ButtonToolbar,
    ButtonGroup,
    Input,
    Label,
    Glyphicon,
    Modal
} from "react-bootstrap";

let {IntlMixin} = ReactIntl;

let TextUnit = React.createClass({

    mixins: [IntlMixin, React.addons.LinkedStateMixin, FluxyMixin],

    statics: {
        storeListeners: {
            "onTextUnitStoreUpdated": TextUnitStore
        }
    },

    propTypes: {
        /** @type {TextUnit} */
        "textUnit": React.PropTypes.object.isRequired
    },

    /**
     *
     * @return {JSX}
     */
    getInitialState() {
        return {
            /** @type {string} */
            "translation": this.props.textUnit.getTarget(),

            /** @type {Boolean} */
            "isEditMode": false,

            /** @type {Boolean} */
            "isShowModal": false,

            /** @type {Boolean} */
            "isErrorAlertShown": false,

            /** @type {TextUnitError} */
            "error": null
        };
    },

    onTextUnitStoreUpdated() {
        let error = TextUnitStore.getError(this.props.textUnit);
        if (error) {
            this.setState({"isErrorAlertShown": true, "error": error});
        } else {
            this.setState({
                "isErrorAlertShown": false,
                "error": null,
                "isEditMode": false
            });
        }
    },

    componentDidUpdate() {
        if (this.state.isEditMode) {
            this.refs.textUnitTextArea.getDOMNode().focus();
        } else {
            this.putFocusOnActiveTextUnit();
        }
    },

    componentDidMount() {
        this.putFocusOnActiveTextUnit();
    },

    putFocusOnActiveTextUnit() {

        if (this.props.isActive) {
            $(this.refs.textunit.getDOMNode()).focus();
        }
    },

    /**
     * When "x" is pressed, select this textunit.
     * @param {SyntheticEvent} e The onKeyUp event object of the textunit root dom node.
     */
    onKeyUpTextUnit(e) {
        let eventKeyCode = keycode(e);
        let resultsStoreState = SearchResultsStore.getState();
        let paramsStoreState = SearchParamsStore.getState();
        switch (eventKeyCode) {
            case "x":
                WorkbenchActions.textUnitSelection(this.getTextUnitFromProps());
                break;
        }
    },

    /**
     * If CTRL+Enter key is pressed, then save the translation of this textunit.
     * @param {SyntheticEvent} e The keyUp event object of the textarea.
     */
    onKeyUpTextArea(e) {
        e.stopPropagation();
        if (keycode(e) == keycode(keycode("enter")) && e.ctrlKey) {
            e.preventDefault();
            this.saveTextUnitIfNeeded(e);
        } else if (keycode(e) == keycode(keycode("escape"))) {
            this.cancelSaveTextUnit(e);
        }
    },

    /**
     * Prevents default behavior on keyDown on textarea if ctrl key is pressed.
     * For ex: If enter key is pressed along with CTRL key, this will prevent appending
     * a newline in the textarea.
     * @param {SyntheticEvent} e The keyDown event object on the textarea
     */
    onKeyDownTextArea(e) {
        e.stopPropagation();
        if (e.ctrlKey) {
            e.preventDefault();
        }
    },

    /**
     * Adds a textUnitIndex attribute to the event and sets it to the
     * index of this textunit in the SearchResults array. The textUnitIndex is
     * used to make this TextUnit active.
     * @param {SyntheticEvent} e The onChange event of the checkbox.
     */
    onChangeTextUnitCheckbox(e) {
        e.textUnitIndex = this.props.textUnitIndex;
        WorkbenchActions.textUnitSelection(this.getTextUnitFromProps());
    },
    /**
     * Prepares the TextUnit for saving
     * @return {TextUnit}
     */
    prepTextUnitForSaving: function () {
        let textUnit = this.getTextUnitFromProps();
        textUnit.setIncludedInLocalizedFile(true);
        textUnit.setStatus(TextUnitSDK.STATUS.APPROVED);
        textUnit.setTarget(this.state.translation);
        return textUnit;
    },
    /**
     * Checks if the textUnit was changed and if so set the new target, reset needsReview and includedInFile state
     * and propagate to the action.
     *
     */
    saveTextUnitIfNeeded(e) {

        e.stopPropagation();
        //TODO Later we'll have to check more that the target...
        if (this.hasTargetChanged()) {
            var textUnit = this.prepTextUnitForSaving();
            WorkbenchActions.checkAndSaveTextUnit(textUnit);
        } else {
            console.log('TextUnit wasn\'t changed');
        }
    },

    cancelSaveTextUnit(e) {
        e.stopPropagation();
        if (this.state.isEditMode) {
            this.setState({
                "isEditMode": false,
                "translation": this.props.textUnit.getTarget(),
            });
        }

    },

    /**
     * Checks if the target has changed by comparing the state with the original text unit provided via the props
     *
     */
    hasTargetChanged() {
        return this.props.textUnit.getTarget() !== this.state.translation;
    },

    /**
     * Gets the textUnit form the state.
     *
     * @returns {TextUnit}
     */
    getTextUnitFromProps() {
        return this.props.textUnit;
    },

    /**
     * Displays the confirmation modal dialog for delete and review actions
     * @param {object} e - The click event object
     */
    onTextUnitGlyphClicked(e) {

        e.stopPropagation();
        this.setState({
            "isShowModal": true
        });
    },

    /**
     * Updates the textunit with comments and performs the chosen action on the modal popup.
     * @param {comment: string, textUnitAction: "review"/"reject"/"accept"/""} - The data sent by TextUnitsActionModal.
     */
    performActionOnTextUnit(modalData) {

        // TODO: web service does not make needsReview and includedInFile mutually exclusive.
        // Set it manually in the UI till the web service is fixed.
        let textUnit = this.getTextUnitFromProps();
        textUnit.setTargetComment(modalData.comment);
        switch (modalData.textUnitAction) {
            case "reject":
                textUnit.setIncludedInLocalizedFile(false);
                textUnit.setStatus(TextUnitSDK.STATUS.TRANSLATION_NEEDED);
                break;
            case "review":
                textUnit.setIncludedInLocalizedFile(true);
                textUnit.setStatus(TextUnitSDK.STATUS.REVIEW_NEEDED);
                break;
            case "accept":
                textUnit.setIncludedInLocalizedFile(true);
                textUnit.setStatus(TextUnitSDK.STATUS.APPROVED);
                break;
            case "translate":
                textUnit.setIncludedInLocalizedFile(true);
                textUnit.setStatus(TextUnitSDK.STATUS.TRANSLATION_NEEDED);
                break;
        }

        WorkbenchActions.saveTextUnit(textUnit);
        this.closeModal();
    },

    closeModal() {
        this.setState({
            "isShowModal": false
        });
    },

    /**
     * Returns the label to show unused status or empty string if textunit is used.
     * @returns {Label}
     */
    renderUnusedLabel() {

        let rendered = '';
        if (!this.props.textUnit.isUsed()) {
            rendered = (
                <Label bsStyle="default" className="mrxs">{this.getIntlMessage("textUnit.unused")}</Label>
            );
        }
        return rendered;
    },

    /**
     * Returns the JSX to show the status of reviewNeeded for textunit
     * @returns {JSX}
     */
    renderReviewGlyph() {

        let ui = "";
        if (this.props.textUnit.isTranslated()) {

            let glyphType = "ok";
            let glyphTitle = this.getIntlMessage("textUnit.reviewModal.accepted");

            if (!this.props.textUnit.isIncludedInLocalizedFile()) {

                glyphType = "alert";
                glyphTitle = this.getIntlMessage("textUnit.reviewModal.rejected");

            } else if (this.props.textUnit.getStatus() === TextUnitSDK.STATUS.REVIEW_NEEDED) {

                glyphType = "eye-open";
                glyphTitle = this.getIntlMessage("textUnit.reviewModal.needsReview");

            } else if (this.props.textUnit.getStatus() === TextUnitSDK.STATUS.TRANSLATION_NEEDED) {

                glyphType = "edit";
                glyphTitle = this.getIntlMessage("textUnit.reviewModal.translationNeeded");
            }
            ui = (
                <Glyphicon glyph={glyphType} id="reviewStringButton" title={glyphTitle} className="btn"
                           onClick={this.onTextUnitGlyphClicked}/>
            );
        }

        return ui;
    },

    /**
     * @returns {JSX} - This function returns the UI for the edit mode of the target string area
     * of this text unit
     */
    getUIForEditMode() {
        if (this.state.isEditMode) {
            let saveDisabled = !this.hasTargetChanged();

            // using the target language direction for type the translation but it is improper to display the placeholder
            // which should be displayed with the UI direction.
            // This issue is minor, placeholder is "New translation..." only the dots gets pushed on the left side.
            let dir = Locales.getLanguageDirection(this.props.textUnit.getTargetLocale());

            return (
                <div className="targetstring-container">
                    <textarea ref="textUnitTextArea" spellCheck="true" className="form-control mrxs"
                              onKeyUp={this.onKeyUpTextArea} onKeyDown={this.onKeyDownTextArea}
                              placeholder={this.getIntlMessage('textUnit.target.placeholder')}
                              valueLink={this.linkState("translation")} dir={dir}/>

                    <ButtonToolbar className="mtxs mbxs">
                        <Button bsStyle='primary' bsSize="small" disabled={saveDisabled}
                                onClick={!saveDisabled ? this.saveTextUnitIfNeeded : null}>
                            {this.getIntlMessage('label.save')}
                        </Button>
                        <Button bsSize="small" onClick={this.cancelSaveTextUnit}>
                            {this.getIntlMessage("label.cancel")}
                        </Button>
                    </ButtonToolbar>
                </div>
            );
        }
    },

    /**
     * Turns on the edit mode for the target string area of the textunit.
     * @param {object} e - The click event object.
     */
    editStringClicked(e) {
        e.stopPropagation();
        this.setState({
            isEditMode: true
        });
    },

    /**
     * @returns {JSX} - This area has a readonly mode and an edit mode. This function
     * returns the JSX for the target string area depending on the view mode.
     */
    getTargetStringUI() {
        let ui;
        if (this.state.isEditMode) {
            ui = this.getUIForEditMode();
        } else {
            let targetString = this.props.textUnit.getTarget();
            let dir;

            let noTranslation = false;
            let targetClassName = "pts textunit-string textunit-target";
            if (targetString == null) {
                noTranslation = true;
                dir = Locales.getLanguageDirection(Locales.getCurrentLocale());
                targetClassName = targetClassName + " color-gray-light2";
                targetString = this.getIntlMessage("textUnit.target.enterNewTranslation");
            } else {
                dir = Locales.getLanguageDirection(this.props.textUnit.getTargetLocale());
            }

            ui = (
                <label className={targetClassName} onClick={this.editStringClicked} dir={dir}>
                    {targetString}
                </label>
            );
        }
        return ui;
    },

    /**
     * Handle locale label click
     */
    onLocaleLabelClick() {
        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": SearchParamsStore.getState().repoIds,
            "bcp47Tags": [this.props.textUnit.getTargetLocale()],
        });
    },

    onStringIdClick() {
        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": SearchParamsStore.getState().repoIds,
            "searchText": this.props.textUnit.getName(),
            "searchAttribute": SearchParamsStore.SEARCH_ATTRIBUTES.STRING_ID,
            "searchType": SearchParamsStore.SEARCH_TYPES.EXACT,
            "bcp47Tags": RepositoryStore.getAllBcp47TagsForRepositoryIds(SearchParamsStore.getState().repoIds),
        });
    },

    /**
     * Handling TextUnit onClick event
     * @param {SyntheticEvent} e
     */
    onTextUnitClick(e) {

        switch (e.target) {
            case this.refs.selectCheckbox.getDOMNode():
                this.onChangeTextUnitCheckbox(e);
                break;

            case this.refs.localeLabel.getDOMNode():
                this.onLocaleLabelClick(e);
                break;

            case this.refs.stringId.getDOMNode():
                this.onStringIdClick(e);
                break;

            case typeof this.refs.textUnitTextArea !== "undefined" && this.refs.textUnitTextArea.getDOMNode():
                break;

            default:
                this.onChangeTextUnitCheckbox(e);
        }
    },

    /**
     * @returns {JSX} The JSX for the TextUnitsReviewModal if isShowModal is true, empty string otherwise.
     */
    getTextUnitReviewModal() {
        let ui = "";
        if (this.state.isShowModal) {
            let textUnitArray = [this.getTextUnitFromProps()];
            ui = (
                <TextUnitsReviewModal isShowModal={this.state.isShowModal}
                                      onReviewModalSaveClicked={this.performActionOnTextUnit}
                                      onCloseModal={this.closeModal} textUnitsArray={textUnitArray}/>
            );
        }
        return ui;
    },

    handleErrorAlertDismiss() {
        this.setState({
            "isErrorAlertShown": false,
        });
    },

    handleModalSave() {
        var textUnit = this.prepTextUnitForSaving();
        WorkbenchActions.saveTextUnit(textUnit);

        this.setState({
            "isErrorAlertShown": false,
        });
    },

    /**
     * Retrieves the message key from Error.MESSAGEKEYS_MAP, populates it with values for any parameters it may
     * have and then returns the error message.
     * @param {TextUnitError} error
     * @returns {string} The error message with all the parameters populated.
     */
    getErrorMessage(error) {
        return this.getIntlMessage(Error.MESSAGEKEYS_MAP[error.errorId]);
    },

    getErrorAlert() {
        if (this.state.isErrorAlertShown) {
            let buttons;
            if (this.state.error.errorId == Error.IDS.TEXTUNIT_CHECK_FAILED) {
                buttons = (
                    <div>
                        <Button bsStyle="primary" onClick={this.handleModalSave}>
                            {this.getIntlMessage("label.yes")}
                        </Button>
                        <Button onClick={this.handleErrorAlertDismiss}>
                            {this.getIntlMessage("label.no")}
                        </Button>
                    </div>
                );
            } else {
                buttons = (
                    <Button onClick={this.handleErrorAlertDismiss}>
                        {this.getIntlMessage("label.okay")}
                    </Button>);
            }

            return (
                <Modal show={true} onHide={this.handleErrorAlertDismiss}>
                    <Modal.Header closeButton>
                        <Modal.Title>{this.getIntlMessage("error.modal.title")}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>{this.getErrorMessage(this.state.error)}</Modal.Body>
                    <Modal.Footer>
                        {buttons}
                    </Modal.Footer>
                </Modal>
            );
        }
    },
    
    /**
     * render the source. If the source ends with a retrun line remove and 
     * render a return line symbol so that the user as a clue about the trailing
     * return line.
     */
    renderSource() {
        
        let source = this.props.textUnit.getSource();
        let optionalReturnLineSymbol = "";
        
        if (source.endsWith("\n")) {
            source = source.substring(0, source.length - 1); 
            optionalReturnLineSymbol = (
                    <span className="textunit-returnline"> ↵</span> 
            );
        }
        
        return (
            <div className="plx pts textunit-string">{source}{optionalReturnLineSymbol}
            </div>
        );
    },

    render() {
        // TODO: Must show which repository a string belongs to when multiple repositories are selected
        let textunitClass = "mrm pbm ptm textunit";
        let isActive = this.props.isActive;
        let isSelected = this.props.isSelected;
        if (isActive) {
            textunitClass = textunitClass + " textunit-active";
        }
        if (isSelected) {
            textunitClass = textunitClass + " textunit-selected";
        }

        return (
            <div ref="textunit" className={textunitClass} onKeyUp={this.onKeyUpTextUnit} tabIndex={0}
                 onClick={this.onTextUnitClick}>
                {this.getErrorAlert()}
                <div>
                    <Grid fluid={true}>
                        <Row className='show-grid'>
                            <Col xs={11}>
                                <span className="mrxs">
                                    <input type="checkbox" ref="selectCheckbox" checked={isSelected}/>
                                </span>
                                <Label bsStyle='primary' bsSize='large' className="mrxs mtl clickable"
                                       ref="localeLabel">
                                    {this.props.textUnit.getTargetLocale()}
                                </Label>
                                {this.renderUnusedLabel()}
                                <span className="clickable" ref="stringId">{this.props.textUnit.getName()}</span>
                            </Col>
                        </Row>
                        <Row className='show-grid'>
                            <Col md={6}>
                                <Row>
                                   {this.renderSource()}                                                               
                                    <div className="plx em color-gray-light2">{this.props.textUnit.getComment()}</div>
                                </Row>
                            </Col>
                            <Col md={6}>
                                <Row>
                                    {this.getTargetStringUI()}
                                    <span className="textunit-actionbar mrxs">{this.renderReviewGlyph()}</span>
                                </Row>
                            </Col>
                        </Row>
                    </Grid>
                </div>
                {this.getTextUnitReviewModal()}
            </div>
        );
    }
});

export default TextUnit;
