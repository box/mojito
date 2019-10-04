import _ from "lodash";
import keycode from "keycode";
import FluxyMixin from "alt-mixins/FluxyMixin";
import PropTypes from 'prop-types';
import React from "react";
import createReactClass from 'create-react-class';
import ReactDOM from "react-dom";
import {FormattedMessage, injectIntl} from "react-intl";
import Error from "../../utils/Error";
import SearchConstants from "../../utils/SearchConstants";
import SearchResultsStore from "../../stores/workbench/SearchResultsStore";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import RepositoryStore from "../../stores/RepositoryStore";
import TextUnitStore from "../../stores/workbench/TextUnitStore";
import TextUnitsReviewModal from "./TextUnitsReviewModal";
import TextUnitSDK from "../../sdk/TextUnit";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import GitBlameActions from "../../actions/workbench/GitBlameActions";
import TranslationHistoryActions from "../../actions/workbench/TranslationHistoryActions";
import Locales from "../../utils/Locales";
import {
    Grid,
    Row,
    Col,
    Button,
    ButtonToolbar,
    FormControl,
    Input,
    Label,
    Glyphicon,
    Modal,
    OverlayTrigger,
    Tooltip
} from "react-bootstrap";


let TextUnit = createReactClass({
    displayName: 'TextUnit',
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onTextUnitStoreUpdated": TextUnitStore
        }
    },

    propTypes: {
        /** @type {TextUnit} */
        "textUnit": PropTypes.object.isRequired,

        /** @type {string} Since we don't have immutable objects, and the textunit object is mutated everywhere, this is
         * the best way to ensure we get the freshest translation from the parent.  Always use this instead of the textunit.getTarget()
         * The benefit here is that when the translation prop changes, the component will automatically re-render
         */
        "translation": PropTypes.string,

        /** @type {function} */
        "onEditModeSetToTrue": PropTypes.func,

        /** @type {function} */
        "onEditModeSetToFalse": PropTypes.func,

        /** @type {number} */
        "textUnitIndex": PropTypes.number,

        /** @type {boolean} */
        "isActive": PropTypes.bool
    },

    /**
     *
     * @return {JSX}
     */
    getInitialState() {
        return {
            /** @type {string} This state should only be used to store the state of edited translation only */
            "translation": this.props.translation,

            /** @type {Boolean} */
            "isEditMode": false,

            /** @type {Boolean} */
            "isShowModal": false,

            /** @type {Boolean} */
            "isErrorAlertShown": false,

            /** @type {Boolean} */
            "isCancelConfirmShown": false,

            /** @type {TextUnitError} */
            "error": null
        };
    },

    /**
     * Invoked when a component is receiving new props. This method is not called for the initial render.
     *
     * Use this as an opportunity to react to a prop transition before render() is called by updating the state using
     * this.setState(). The old props can be accessed via this.props. Calling this.setState() within this function
     * will not trigger an additional render.
     * @param nextProps
     */
    componentWillReceiveProps(nextProps) {
        // update translation state if the new props is different and it's not currently being edited.
        if (!this.state.isEditMode && nextProps.translation !== this.props.translation) {
            this.setState({ "translation": nextProps.translation });
        }
    },

    /**
     * @param {object} nextProps
     * @param {object} nextState
     * @return {boolean}
     */
    shouldComponentUpdate(nextProps, nextState) {
        return !_.isEqual(this.props, nextProps) || !_.isEqual(this.state, nextState);
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
            }, () => {
                if (this.props.onEditModeSetToFalse) {
                    this.props.onEditModeSetToFalse(this);
                }
            });
        }
    },

    componentDidUpdate() {
        if (this.state.isEditMode) {
            ReactDOM.findDOMNode(this.refs.textUnitTextArea).focus();
        } else {
            this.putFocusOnActiveTextUnit();
        }
    },

    componentDidMount() {
        this.putFocusOnActiveTextUnit();
    },

    putFocusOnActiveTextUnit() {
        if (this.props.isActive) {
           this.refs.textunit.focus();
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
                WorkbenchActions.textUnitSelection(this.getCloneOfTextUnitFromProps());
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
            this.cancelEditTextUnitHandlerEvent(e);
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
        if (keycode(e) == keycode(keycode("enter")) && e.ctrlKey) {
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
        e.persist();
        e.textUnitIndex = this.props.textUnitIndex;
        WorkbenchActions.textUnitSelection(this.getCloneOfTextUnitFromProps());
    },

    /**
     * Prepares the TextUnit for saving
     * @return {TextUnit}
     */
    prepTextUnitForSaving: function () {

        let textUnit = this.getCloneOfTextUnitFromProps();

        textUnit.setIncludedInLocalizedFile(true);
        textUnit.setStatus(TextUnitSDK.STATUS.APPROVED);
        textUnit.setTarget(this.state.translation);
        textUnit.setTranslated(true);
        
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

    /**
     * @param {Event} e
     */
    cancelEditTextUnitHandlerEvent(e) {
        e.stopPropagation();
        this.cancelEditTextUnitHandler();
    },

    /**
     * The pending Promise.resolve that is to be called when cancellation occurs or rejected.
     * wasSuccessfullyCancelled is true if it cancellation of edit mode was successful.  Otherwise, it is false.
     *
     * This resolve is part of the Promise that is return when {@link cancelEditTextUnitHandler} is called.
     *
     * @typedef {Boolean} wasSuccessfullyCancelled
     * @type {function(wasSuccessfullyCancelled)}
     */
    pendingCancelEditPromiseResolve: null,

    /**
     * @return {Promise.<Boolean>}
     */
    cancelEditTextUnitHandler() {
        return new Promise((resolve, reject) => {
            this.pendingCancelEditPromiseResolve = resolve;

            if (this.hasTargetChanged()) {
                this.setState({"isCancelConfirmShown": true});
            } else {
                this.doCancelEditTextUnit();
            }
        });
    },

    /**
     * Actually cancel the edit model and reset the translation to the original translation
     */
    doCancelEditTextUnit() {
        if (this.state.isEditMode) {
            this.setState({
                "isEditMode": false,
                "translation": this.props.translation
            }, () => {
                if (this.props.onEditModeSetToFalse) {
                    this.props.onEditModeSetToFalse(this);
                }

                if (this.pendingCancelEditPromiseResolve) {
                    this.pendingCancelEditPromiseResolve(true);
                    this.pendingCancelEditPromiseResolve = null;
                }
            });
        }
    },

    /**
     * Abandon the cancellation of edit mode
     */
    abandonEditTextUnitCancellation() {
        if (this.pendingCancelEditPromiseResolve) {
            this.pendingCancelEditPromiseResolve(false);
            this.pendingCancelEditPromiseResolve = null;
        }
    },

    /**
     * Checks if the target has changed by comparing the state with the original text unit provided via the props
     *
     */
    hasTargetChanged() {
        return this.props.translation !== this.state.translation;
    },

    /**
     * Gets a clone of the textUnit form the props
     *
     * @returns {TextUnit}
     */
    getCloneOfTextUnitFromProps() {
        return TextUnitSDK.toTextUnit(_.clone(this.props.textUnit.data));
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
        let textUnit = this.getCloneOfTextUnitFromProps();

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
                <Label bsStyle="default" className="mrxs"><FormattedMessage id="textUnit.unused"/></Label>
            );
        }
        return rendered;
    },

    /**
     * Returns the label to show do translate status or empty string if textunit is to be translated.
     * @returns {Label}
     */
    renderDoNotTranslateLabel() {

        let rendered = '';
        if (this.props.textUnit.getDoNotTranslate()) {
            rendered = (
                <Label bsStyle="default" className="mrxs"><FormattedMessage id="textUnit.doNotTranslate"/></Label>
            );
        }
        return rendered;
    },

    /**
     * Returns the label to show plural form or empty string if textunit has none.
     * @returns {Label}
     */
    renderPluralFormLabel() {

        let rendered = '';
        if (this.props.textUnit.getPluralForm() != null) {
            rendered = (
                <Label bsStyle="default" className="mrxs clickable" onClick={this.onPluralFormLabelClick}>
                    {this.props.textUnit.getPluralForm()}
                </Label>
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
            let glyphTitle = this.props.intl.formatMessage({id: "textUnit.reviewModal.accepted"});

            if (!this.props.textUnit.isIncludedInLocalizedFile()) {

                glyphType = "alert";
                glyphTitle = this.props.intl.formatMessage({id: "textUnit.reviewModal.rejected"});

            } else if (this.props.textUnit.getStatus() === TextUnitSDK.STATUS.REVIEW_NEEDED) {

                glyphType = "eye-open";
                glyphTitle = this.props.intl.formatMessage({id: "textUnit.reviewModal.needsReview"});

            } else if (this.props.textUnit.getStatus() === TextUnitSDK.STATUS.TRANSLATION_NEEDED) {

                glyphType = "edit";
                glyphTitle = this.props.intl.formatMessage({id: "textUnit.reviewModal.translationNeeded"});
            }
            ui = (
                <Glyphicon glyph={glyphType} id="reviewStringButton" title={glyphTitle} className="btn"
                           onClick={this.onTextUnitGlyphClicked}/>
            );
        }

        return ui;
    },

    /**
     * @param {Event} event
     */
    transUnitEditTextAreaOnChange(event) {
        this.setState({
            "translation": event.target.value
        });
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

            let defaultTextAreaValue = "";
            if (this.state.translation) {
                defaultTextAreaValue = this.state.translation;
            } else if (this.props.translation) {
                defaultTextAreaValue = this.props.translation;
            }

            return (
                <div className="targetstring-container">
                    <FormControl ref="textUnitTextArea" componentClass="textarea" spellCheck="true" className="mrxs"
                                 onKeyUp={this.onKeyUpTextArea} onKeyDown={this.onKeyDownTextArea}
                                 placeholder={this.props.intl.formatMessage({ id: 'textUnit.target.placeholder' })}
                                 defaultValue={defaultTextAreaValue}
                                 onChange={this.transUnitEditTextAreaOnChange}
                                 dir={dir}
                                 onClick={this.onClickTextArea}/>

                    <ButtonToolbar className="mtxs mbxs">
                        <Button bsStyle='primary' bsSize="small" disabled={saveDisabled}
                                onClick={!saveDisabled ? this.saveTextUnitIfNeeded : null}>
                            <FormattedMessage id='label.save'/>
                        </Button>
                        <Button bsSize="small" onClick={this.cancelEditTextUnitHandlerEvent}>
                            <span className={this.hasTargetChanged() ? "text-danger" : ""}><FormattedMessage
                                id="label.cancel"/></span>
                        </Button>
                    </ButtonToolbar>
                </div>
            );
        }
    },

    /**
     * Turns on the edit mode for the target string area of the textunit and
     * call the onEditModeSetToTrue callback if defined.
     *
     * @param {object} e - The click event object.
     */
    editStringClicked(e) {
        e.stopPropagation();

        this.setState({
            isEditMode: true
        }, () => {
            if (this.props.onEditModeSetToTrue) {
                this.props.onEditModeSetToTrue(this);
            }
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
            let targetString = this.hasTargetChanged() ? this.state.translation : this.props.translation;
            let dir;
            let leadingWhitespacesSymbol = "";
            let trailingWhitespacesSymbol = "";

            let noTranslation = false;
            let targetClassName = "pts pls pbs textunit-string textunit-target";
            if (targetString == null) {
                noTranslation = true;
                dir = Locales.getLanguageDirection(Locales.getCurrentLocale());
                targetClassName = targetClassName + " color-gray-light2";
                targetString = this.props.intl.formatMessage({id: "textUnit.target.enterNewTranslation"});
            } else {
                dir = Locales.getLanguageDirection(this.props.textUnit.getTargetLocale());
                leadingWhitespacesSymbol = this.getLeadingWhitespacesSymbol(targetString);
                trailingWhitespacesSymbol = this.getTrailingWhitespacesSymbol(targetString);
                targetString = targetString.trim();
            }

            ui = (
                <label className={targetClassName} onClick={this.editStringClicked} dir={dir}>
                {leadingWhitespacesSymbol}{targetString}{trailingWhitespacesSymbol}
                </label>
            );
        }
        return ui;
    },

    /**
     * Handle click on the locale label: stop event propagation (no need to bubble
     * up as we're reloading the workbench with new data) and update the search
     * parameter to show strings for the locale specified on the label
     *
     * @param {SyntheticEvent} e
     */
    onLocaleLabelClick(e) {

        e.stopPropagation();

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": SearchParamsStore.getState().repoIds,
            "bcp47Tags": [this.props.textUnit.getTargetLocale()],
        });
    },

    /**
     * Handle click on the plural form label: stop event propagation (no need to 
     * bubble up as we're reloading the workbench with new data) and update the 
     * search parameter to show all plural forms for the string 
     * 
     * @param {SyntheticEvent} e
     */
    onPluralFormLabelClick(e) {

        e.stopPropagation();

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": SearchParamsStore.getState().repoIds,
            "searchText": this.props.textUnit.getPluralFormOther(),
            "searchAttribute": SearchParamsStore.SEARCH_ATTRIBUTES.PLURAL_FORM_OTHER,
            "searchType": SearchParamsStore.SEARCH_TYPES.EXACT,
            "bcp47Tags": [this.props.textUnit.getTargetLocale()]
        });
    },

    /**
     * Stop event propagation when clicking on the text area in edit mode (we
     * don't want that click to select/unselect the textunit).
     *
     * @param {SyntheticEvent} e
     */
    onClickTextArea(e) {
        e.stopPropagation();
    },

    /**
     * Handle click on the string id: stop event propagation (no need to bubble
     * up as we're reloading the workbench with new data) and update the search
     * parameter to show strings for the given string id.
     *
     * @param {SyntheticEvent} e
     */
    onStringIdClick(e) {

        e.stopPropagation();

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": SearchParamsStore.getState().repoIds,
            "searchText": this.props.textUnit.getName(),
            "searchAttribute": SearchParamsStore.SEARCH_ATTRIBUTES.STRING_ID,
            "searchType": SearchParamsStore.SEARCH_TYPES.EXACT,
            "bcp47Tags": RepositoryStore.getAllBcp47TagsForRepositoryIds(SearchParamsStore.getState().repoIds),
        });
    },

    onTextUnitInfoClick(e){

        e.stopPropagation();

        GitBlameActions.openWithTextUnit(this.props.textUnit);
    },

    onTranslationHistoryClick(e){

        e.stopPropagation();

        TranslationHistoryActions.openWithTextUnit(this.props.textUnit);
    },

    /**
     * Handle click on the asset path icon: stop event propagation (no need to bubble
     * up as we're reloading the workbench with new data) and update the search
     * parameter to show strings for the given string id.
     *
     * @param {SyntheticEvent} e
     */
    onAssetPathClick(e) {

        e.stopPropagation();

        WorkbenchActions.searchParamsChanged({
            "changedParam": SearchConstants.UPDATE_ALL,
            "repoIds": SearchParamsStore.getState().repoIds,
            "searchText": this.props.textUnit.getAssetPath(),
            "searchAttribute": SearchParamsStore.SEARCH_ATTRIBUTES.ASSET,
            "searchType": SearchParamsStore.SEARCH_TYPES.EXACT,
            "bcp47Tags": [this.props.textUnit.getTargetLocale()]
        });
    },

    /**
     * Handling TextUnit onClick event
     * @param {SyntheticEvent} e
     */
    onTextUnitClick(e) {
        // NOTE: if text has been selected for this textunit, don't activate it because the user's intention is to
        // select text, not activate textunit.
        if (!window.getSelection().toString()) {
            this.onChangeTextUnitCheckbox(e);
        }
    },

    /**
     * @returns {JSX} The JSX for the TextUnitsReviewModal if isShowModal is true, empty string otherwise.
     */
    getTextUnitReviewModal() {
        let ui = "";
        if (this.state.isShowModal) {
            let textUnitArray = [this.getCloneOfTextUnitFromProps()];
            ui = (
                <TextUnitsReviewModal isShowModal={this.state.isShowModal}
                                      onReviewModalSaveClicked={this.performActionOnTextUnit}
                                      onCloseModal={this.closeModal} textUnitsArray={textUnitArray}/>
            );
        }
        return ui;
    },

    handleErrorAlertDismiss() {

        WorkbenchActions.resetErrorState(this.props.textUnit);
 
        this.setState({
            "isErrorAlertShown": false,
            "isEditMode" : true
        });
    },

    handleModalSave() {
        var textUnit = this.prepTextUnitForSaving();
        WorkbenchActions.saveTextUnit(textUnit);

        this.setState({
            "isErrorAlertShown": false
        });
    },

    /**
     * Retrieves the message key from Error.MESSAGEKEYS_MAP, populates it with values for any parameters it may
     * have and then returns the error message.
     * @param {TextUnitError} error
     * @returns {string} The error message with all the parameters populated.
     */
    getErrorMessage(error) {
        return this.props.intl.formatMessage({id: Error.MESSAGEKEYS_MAP[error.errorId]});
    },

    getErrorAlert() {
        if (this.state.isErrorAlertShown) {
            let buttons;
            if (this.state.error.errorId == Error.IDS.TEXTUNIT_CHECK_FAILED) {
                buttons = (
                    <div>
                        <Button bsStyle="primary" onClick={this.handleModalSave}>
                            <FormattedMessage id="label.yes"/>
                        </Button>
                        <Button onClick={this.handleErrorAlertDismiss}>
                            <FormattedMessage id="label.no"/>
                        </Button>
                    </div>
                );
            } else {
                buttons = (
                    <Button onClick={this.handleErrorAlertDismiss}>
                        <FormattedMessage id="label.okay"/>
                    </Button>);
            }

            return (
                <Modal show={true} onHide={this.handleErrorAlertDismiss}>
                    <Modal.Header closeButton>
                        <Modal.Title><FormattedMessage id="error.modal.title"/></Modal.Title>
                    </Modal.Header>
                    <Modal.Body>{this.getErrorMessage(this.state.error)}</Modal.Body>
                    <Modal.Footer>
                        {buttons}
                    </Modal.Footer>
                </Modal>
            );
        }
    },

    getLeadingWhitespacesSymbol(str) {
        let optionalWhitespaceSymbols = "";
        let leadingWhitespacesRegex = /^(\s+).*?/g;
        let match = leadingWhitespacesRegex.exec(str);
        if (match) {
            optionalWhitespaceSymbols = this.getWhitespacesSymbol(match[1]);
        }
        return optionalWhitespaceSymbols;
    },

    getTrailingWhitespacesSymbol(str) {
        let optionalWhitespaceSymbols = "";
        let trailingWhitespacesRegex = /.*?(\s+)$/g;
        let match = trailingWhitespacesRegex.exec(str);
        if (match) {
            optionalWhitespaceSymbols = this.getWhitespacesSymbol(match[1]);
        }
        return optionalWhitespaceSymbols;
    },

    getWhitespacesSymbol(str) {
        let whitespaces = str.replace(/\n/g, "↵");
        whitespaces = whitespaces.replace(/ /g, "⎵");
        return (
            <span className="textunit-whitespaces">{whitespaces}</span>
        );
    },

    /**
     * render the source. If the source ends with a retrun line remove and
     * render a return line symbol so that the user as a clue about the trailing
     * return line.
     */
    renderSource() {

        let source = this.props.textUnit.getSource();
        let leadingWhitespacesSymbol = this.getLeadingWhitespacesSymbol(source);
        let trailingWhitespacesSymbol = this.getTrailingWhitespacesSymbol(source);
        source = source.trim();

        return (
            <div className="plx pts textunit-string">{leadingWhitespacesSymbol}{source}{trailingWhitespacesSymbol}</div>
        );
    },

    renderName() {
        let assetPathWithZeroWidthSpace = this.addZeroWidthSpace(this.props.textUnit.getAssetPath()); // to make the tooltip text to wrap
        let assetPathTooltip = <Tooltip id="{this.props.textUnit.getId()}-assetPath">{assetPathWithZeroWidthSpace}</Tooltip>;
        let assetPathWithGitInfoTooltip =
            <Tooltip id="{this.props.textUnit.getId()}-gitInfo">{this.props.intl.formatMessage( {id: 'workbench.gitBlameModal.info'} )}</Tooltip>;

        let assetPathTranslationHistoryTooltip =
            <Tooltip id="{this.props.textUnit.getId()}-translation-history">{this.props.intl.formatMessage( {id: 'workbench.translationHistoryModal.info'} )}</Tooltip>;

        return (<span className="clickable textunit-name"
                      onClick={this.onStringIdClick}>
                    <span>{this.props.textUnit.getName()}</span>
                    <OverlayTrigger placement="top" overlay={assetPathTooltip}>
                        <span className="textunit-assetpath glyphicon glyphicon-level-up mls" 
                               onClick={this.onAssetPathClick} />
                    </OverlayTrigger>

                    <OverlayTrigger placement="top" overlay={assetPathWithGitInfoTooltip}>
                        <span className="textunit-gitInfo glyphicon glyphicon-info-sign mls"
                              onClick={this.onTextUnitInfoClick} />
                    </OverlayTrigger>

                    <OverlayTrigger placement="top" overlay={assetPathTranslationHistoryTooltip}>
                        <span className="textunit-translation-history glyphicon glyphicon-calendar mls"
                              onClick={this.onTranslationHistoryClick} />
                    </OverlayTrigger>
                </span>
        );
    },

    addZeroWidthSpace(string) {
        
        let newString = "";
        
        [...string].forEach(c => {
            if (newString.length % 10 === 0) {
                newString += '\u200B';
            }        
            newString += c;
        });
        
        return newString;
    },

    handleConfirmCancel() {
        this.setState({"isCancelConfirmShown": false});
        this.doCancelEditTextUnit();
    },

    handleConfirmCancelNo() {
        this.setState({"isCancelConfirmShown": false});
        this.abandonEditTextUnitCancellation();
    },

    /**
     * @return {JSX}
     */
    getCancelConfirmationModel() {
        let result = null;

        if (this.state.isCancelConfirmShown) {
            result = (
                <Modal show={true} onHide={this.handleCancelConfirmationDismiss}>
                    <Modal.Header closeButton>
                        <Modal.Title><FormattedMessage id="modal.title"/></Modal.Title>
                    </Modal.Header>
                    <Modal.Body><FormattedMessage id="textUnit.cancel.confirmation"/></Modal.Body>
                    <Modal.Footer>
                        <Button bsStyle="primary" onClick={this.handleConfirmCancel}>
                            <FormattedMessage id="label.yes"/>
                        </Button>
                        <Button onClick={this.handleConfirmCancelNo}>
                            <FormattedMessage id="label.no"/>
                        </Button>
                    </Modal.Footer>
                </Modal>
            );
        }

        return result;
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
                                    <input type="checkbox" checked={isSelected} readOnly={true}/>
                                </span>
                                <Label bsStyle='primary' bsSize='large' className="mrxs mtl clickable"
                                       onClick={this.onLocaleLabelClick}>
                                    {this.props.textUnit.getTargetLocale()}
                                </Label>
                                {this.renderUnusedLabel()}
                                {this.renderDoNotTranslateLabel()}
                                {this.renderPluralFormLabel()}
                                {this.renderName()}
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
                {this.getCancelConfirmationModel()}
            </div>
        );
    },
});

export default injectIntl(TextUnit);
