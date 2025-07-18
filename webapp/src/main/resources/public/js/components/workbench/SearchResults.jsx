import _ from "lodash";
import FluxyMixin from "alt-mixins/FluxyMixin";
import keycode from "keycode";
import React, { Fragment } from "react";
import createReactClass from "create-react-class";
import { FormattedMessage, injectIntl } from "react-intl";
import {
    Button,
    ButtonToolbar,
    DropdownButton,
    MenuItem,
} from "react-bootstrap";
import Error from "../../utils/Error";
import DeleteConfirmationModal from "../widgets/DeleteConfirmationModal";
import ErrorModal from "../widgets/ErrorModal";
import SearchConstants from "../../utils/SearchConstants";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchResultsStore from "../../stores/workbench/SearchResultsStore";
import TextUnit from "./TextUnit";
import TextUnitsReviewModal from "./TextUnitsReviewModal";
import TranslateModal from "./TranslateModal";
import TextUnitSelectorCheckBox from "./TextUnitSelectorCheckBox";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import ReviewTextUnitsDTO from "../../stores/workbench/ReviewTextUnitsDTO";
import TextUnitSDK from "../../sdk/TextUnit";
import AltContainer from "alt-container";
import ViewModeStore from "../../stores/workbench/ViewModeStore";
import ViewModeDropdown from "./ViewModeDropdown";
import ViewModeActions from "../../actions/workbench/ViewModeActions.js";
import AuthorityService from "../../utils/AuthorityService";
import DelayedSpinner from "../common/DelayedSpinner";
import magnifyingGlassSvg from "../../../img/magnifying-glass.svg";

let SearchResults = createReactClass({
    displayName: 'SearchResults',
    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onSearchResultsStoreUpdated": SearchResultsStore
        }
    },

    /**
     * @return {{searchResults: (*|Array|TextUnit[]), searchHadNoResults: boolean, noMoreResults: boolean, mustShowToolbar: boolean, currentPageNumber: (*|Number|number), activeTextUnitIndex: number, showDeleteModal: boolean, mustShowReviewModal: boolean, isErrorOccurred: boolean, errorObject: null, errorResponse: null, textUnitInEditMode: null}}
     */
    getInitialState() {
        let resultsStoreState = SearchResultsStore.getState();
        let searchParamsStoreState = SearchParamsStore.getState();
        return {
            /** @type [] Array of textunits in the current page of the search results. */
            searchResults: resultsStoreState.searchResults,

            /** @type {Boolean} */
            isSearching: SearchResultsStore.getState().isSearching,

            /** @type {Boolean} True if the minimum set of parameters are required for searching */
            isReadyForSearching: false,

            /** @type {Boolean} True when search didn't result any result, It's different than searchResults.length equals to 0 b'c it can be 0 if search has not been requested. */
            searchHadNoResults: false,

            /** @type {Boolean} Indicates that no more results exist for the search criteria. Helps maintain enabled status of toolbar buttons. */
            noMoreResults: false,

            /** @type {Boolean} If no results exist for the provided search criteria, this boolean helps to hide the workbench toolbar. */
            mustShowToolbar: false,

            /** @type {Number} Indicates the current page number of the search results. */
            currentPageNumber: searchParamsStoreState.currentPageNumber,

            /** @type {Number} The number of items on each page. */
            pageSize: searchParamsStoreState.pageSize,

            /** @type {Number}  The index of the currently active textunit. */
            activeTextUnitIndex: 0,

            /** @type {Boolean} Displays the DeleteConfirmationModal when the delete button is clicked. */
            showDeleteModal: false,

            /** @type {Boolean} Helps show the ErrorModal if set to true. */
            mustShowReviewModal: false,

            /** @type {Boolean} Displays the TranslateModal when the delete button is clicked. */
            showTranslateModal: false,

            /** @type {Boolean} */
            isErrorOccurred: false,

            /** @type {Boolean} The Error object created by the store.  */
            errorObject: null,

            /** @type {Boolean} The error object returned by the promise. This can be used to pass parameters to translate the error. See getErrorMessage() in this file for details. */
            errorResponse: null,

            /** @type {TextUnit}   the text unit currently in edit mode if any*/
            textUnitInEditMode: null,

            /** @type {Number} The number of textunits according to the filter */
            filteredItemCount: SearchResultsStore.getState().filteredItemCount,

            /** @type {Boolean} if the count request is still pending */
            isCountRequestPending: SearchResultsStore.getState().isCountRequestPending,
        };
    },

    /**
     * @param {SyntheticEvent} e The event object for the keyUp event
     */
    onKeyUpSearchResults(e) {
        switch (keycode(e)) {
            case "down":
                this.onDownArrowPressed(e);
                break;
            case "up":
                this.onUpArrowPressed(e);
                break;
            case "left":
                this.onFetchPreviousPageClicked();
                break;
            case "right":
                this.onFetchNextPageClicked();
                break;
            case "s":
                this.onStatusTextUnitsClicked();
                break;
            case "d":
                this.onDeleteTextUnitsClicked();
                break;
        }
    },

    /**
     * The onChange event of the checkbox in the textunit bubbles up here. The textunit's index
     * is set on the event object in the checkbox onChange function. It is retrieved here to make the
     * textunit active.
     * @param {SyntheticEvent} e The onChange event object for the event that originated on the
     * checkbox in the textunit.
     */
    onChangeSearchResults(e) {
        if (typeof e.textUnitIndex !== "undefined") {
            this.setActiveTextUnitIndexState(e.textUnitIndex);
        }
    },

    /**
     * Sets the state to activate the textunit after the currently active textunit.
     * If the last textunit in the page was active, then a request is made to load the next page.
     * @param {SyntheticEvent} e The event object passed in from the onKeyUpTextUnit function
     */
    onDownArrowPressed(e) {
        let eventKeyCode = keycode(e);
        let paramsStoreState = SearchParamsStore.getState();
        let activeTextUnitIndex = this.state.activeTextUnitIndex + 1;
        let searchResultsLength = this.state.searchResults.length;
        if (activeTextUnitIndex >= searchResultsLength && !this.state.noMoreResults) {
            WorkbenchActions.searchParamsChanged({
                changedParam: SearchConstants.NEXT_PAGE_REQUESTED,
            });
        } else if (activeTextUnitIndex >= searchResultsLength && this.state.noMoreResults) {
            // boundary condition for pressing down arrow on the last textunit of the last page
            activeTextUnitIndex = searchResultsLength - 1;
        }
        this.setActiveTextUnitIndexState(activeTextUnitIndex);
    },

    /**
     * Sets the state to activate the textunit before the currently active textunit.
     * If the first textunit in the page was active, then a request is made to load the previous page.
     * @param {SyntheticEvent} e The event object passed in from the onKeyUpTextUnit function
     */
    onUpArrowPressed(e) {
        let eventKeyCode = keycode(e);
        let paramsStoreState = SearchParamsStore.getState();
        let activeTextUnitIndex = this.state.activeTextUnitIndex - 1;
        if (activeTextUnitIndex < 0 && paramsStoreState.currentPageNumber > 1) {
            WorkbenchActions.searchParamsChanged({
                "changedParam": SearchConstants.PREVIOUS_PAGE_REQUESTED
            });
        } else if (paramsStoreState.currentPageNumber <= 1 && activeTextUnitIndex < 0) {
            // boundary condition for pressing up arrow on the first textunit of the first page
            activeTextUnitIndex = 0;
        }
        this.setActiveTextUnitIndexState(activeTextUnitIndex);
    },

    /**
     * Calls the action to review selected text units
     */
    onStatusTextUnitsClicked() {
        let selectedTextUnits = this.getSelectedTextUnits();
        if (selectedTextUnits.length >= 1) {
            this.showReviewModal();
        }
    },

    onDoNotTranslateClick() {
        let selectedTextUnits = this.getSelectedTextUnits();
        if (selectedTextUnits.length >= 1) {
            this.showTranslateModal();
        }
    },

    /**
     * Fires a request to review the selected textunits when the user hits the save button on the TextUnitsreviewModal
     * @param {object} modalData
     * @param {string} modalData.textUnitAction One of review/reject/accept
     * @param {string} modalData.comment The text to be set as the targetComment of the selected textunits
     */
    onReviewModalSaveClicked(modalData) {
        this.hideReviewModal();

        let comment = modalData.comment;
        let textUnitAction = modalData.textUnitAction;
        let selectedTextUnits = SearchResultsStore.getSelectedTextUnits();

        WorkbenchActions.reviewTextUnits(new ReviewTextUnitsDTO(selectedTextUnits, comment, textUnitAction));
        WorkbenchActions.resetAllSelectedTextUnits();
    },

    /**
     * Fires a request to clear the selected textunits
     */
    onResetSelectedTexUnitsClicked() {
        WorkbenchActions.resetAllSelectedTextUnits();
    },

    /**
     * Sets the state of this component to show the TextUnitsReviewModal
     */
    showReviewModal() {
        this.setState({
            mustShowReviewModal: true,
        });
    },

    /**
     * Sets the state of this component to hide the TextUnitsReviewModal
     */
    hideReviewModal() {
        this.setState({
            mustShowReviewModal: false,
        });
    },

    /**
     * Sets the state of this component to show the TextUnitsDoNotTranslateModal
     */
    showTranslateModal() {
        this.setState({
            showTranslateModal: true,
        });
    },

    /**
     * Sets the state of this component to hide the TextUnitsDoNotTranslateModal
     */
    hideDoNotTranslateModal() {
        this.setState({
            showTranslateModal: false,
        });
    },

    /**
     * Set the state of this component to show the delete textunits modal
     */
    showDeleteModal() {
        this.setState({
            showDeleteModal: true,
        });
    },

    /**
     * Set the state of this component to hide the delete textunits modal
     */
    hideDeleteModal() {
        this.setState({
            showDeleteModal: false,
        });
    },

    /**
     * @returns {array} An array of selected textunits
     */
    getSelectedTextUnits() {
        let resultsStoreState = SearchResultsStore.getState();
        return _.values(resultsStoreState.selectedTextUnitsMap);
    },

    /**
     * Called when the delete button on the workbench toolbar is clicked. Shows the DeleteConfirmationModal.
     * @param {SyntheticEvent} e The click event object
     */
    onDeleteTextUnitsClicked(e) {
        let selectedTextUnits = this.getSelectedTextUnits();
        if (selectedTextUnits.length >= 1) {
            this.showDeleteModal();
        }
    },

    /**
     * Called when the delete button is clicked on the DeleteConfirmationModal. Performs the delete
     * operation on all the selected textunits.
     */
    onDeleteTextUnitsConfirmed() {
        let selectedTextUnits = SearchResultsStore.getSelectedTextUnits();

        WorkbenchActions.deleteTextUnits(selectedTextUnits);
        WorkbenchActions.resetAllSelectedTextUnits();

        this.hideDeleteModal();
    },

    /**
     * Called when the cancel button is clicked on the DeleteConfirmationModal. Dismisses the modal without
     * performing the delete action.
     */
    onDeleteTextUnitsCancelled() {
        this.hideDeleteModal();
    },

    /**
     * Sets the state of this component to hide the ErrorModal
     */
    onErrorModalClosed() {
        this.setState({
            isErrorOccurred: false,
        });
        WorkbenchActions.resetErrorState();
    },

    onTranslateModalSave(translate) {
        let selectedTextUnits = SearchResultsStore.getSelectedTextUnits();

        for (let selectedTextUnit of selectedTextUnits) {
            let clonedTextUnit = TextUnitSDK.toTextUnit(_.clone(selectedTextUnit.data));
            clonedTextUnit.setDoNotTranslate(!translate);
            WorkbenchActions.saveVirtualAssetTextUnit(clonedTextUnit);
        }

        WorkbenchActions.resetAllSelectedTextUnits();

        this.hideDoNotTranslateModal();
    },

    onTranslateModalCancel() {
        WorkbenchActions.resetAllSelectedTextUnits();
        this.hideDoNotTranslateModal();
    },

    /**
     * Callback called when the SearchResultsStore state is updated.
     * Helps maintain the state of this component depending on the new state.
     * See comments on getInitialState() for how each attribute affects the rendering of this component.
     */
    onSearchResultsStoreUpdated() {
        let resultsStoreState = SearchResultsStore.getState();
        let paramsStoreState = SearchParamsStore.getState();
        let mustShowToolbar = this.mustToolbarBeShown();
        let isReadyForSearching = SearchParamsStore.isReadyForSearching(paramsStoreState);

        this.setState({
            searchResults: resultsStoreState.searchResults,
            isReadyForSearching: isReadyForSearching,
            isSearching: resultsStoreState.isSearching,
            searchHadNoResults: resultsStoreState.searchHadNoResults,
            noMoreResults: resultsStoreState.noMoreResults,
            currentPageNumber: paramsStoreState.currentPageNumber,
            pageSize: paramsStoreState.pageSize,
            mustShowToolbar: mustShowToolbar,
            activeTextUnitIndex: this.getActiveTextUnitIndex(),
            isErrorOccurred: resultsStoreState.isErrorOccurred,
            errorObject: resultsStoreState.errorObject,
            textUnitInEditMode: this.state.textUnitInEditMode,
            filteredItemCount: resultsStoreState.filteredItemCount,
            isCountRequestPending: resultsStoreState.isCountRequestPending
        });
    },

    /**
     * If a text unit is in edit mode, asks for cancel it.  If it is successfully canceled out of edit mode, then
     * save the new provided text unit as the current text unit in edit mode.
     *
     * @param {TextUnit} textUnit component that got edit mode set to true
     */
    onTextUnitEditModeSetToTrue(textUnit) {
        let previous = this.state.textUnitInEditMode;

        if (!previous) {
            this.setStateForEditingTextUnit(textUnit);
        } else if (previous && previous !== textUnit) {
            previous.cancelEditTextUnitHandler().then((hasSuccessfullyCanceled) => {
                if (hasSuccessfullyCanceled) {
                    this.setStateForEditingTextUnit(textUnit);
                } else {
                    textUnit.doCancelEditTextUnit();
                }
            });
        }
    },

    /**
     * Handler for when textunit edit mode is manually canceled
     * @param textUnit
     */
    onTextUnitEditModeSetToFalse(textUnit) {
        // don't need to set "activeTextUnitIndex"
        this.setState({ textUnitInEditMode: null });
    },

    /**
     * @param textUnit component
     */
    setStateForEditingTextUnit(textUnit) {
        this.setState({
            textUnitInEditMode: textUnit,
            activeTextUnitIndex: textUnit.props.textUnitIndex,
        });
    },

    /**
     * Called when the next page button is clicked on the workbench toolbar.
     *  of search results.
     */
    onFetchNextPageClicked() {
        if (!this.state.noMoreResults) {
            this.setState({ textUnitInEditMode: null }, () => {
                WorkbenchActions.searchParamsChanged({
                    changedParam: SearchConstants.NEXT_PAGE_REQUESTED,
                });
            });
        }
    },

    /**
     * Called when the previous page button is clicked on the workbench toolbar.
     * page of search results if the currentPage is greater than one.
     */
    onFetchPreviousPageClicked() {
        if (this.state.currentPageNumber > 1) {
            this.setState({ textUnitInEditMode: null }, () => {
                WorkbenchActions.searchParamsChanged({
                    changedParam: SearchConstants.PREVIOUS_PAGE_REQUESTED,
                });
            });
        }
    },

    /**
     * @param {number} activeTextUnitIndex The index to be set on the state of this component
     */
    setActiveTextUnitIndexState(activeTextUnitIndex) {
        this.setState({
            activeTextUnitIndex: activeTextUnitIndex,
        });
    },

    /**
     * When the up arrow is pressed on the first textunit of the current page,
     * the activeTextUnitIndex will be less than 0. In this case, it is reset to
     * pageSize - 1 to highlight the last textunit in the previous page.
     *
     * When the down arrow is pressed on the last textunit of the current page,
     * the activeTextUnitIndex will be equal to the pageSize. In this case, it is reset to
     * 0 to highlight the first textunit of the next page.
     *
     * @returns {number} The index of the textunit that must be active.
     */
    getActiveTextUnitIndex() {
        let paramsStoreState = SearchParamsStore.getState();
        let activeTextUnitIndex = this.state.activeTextUnitIndex;
        let pageSize = paramsStoreState.pageSize;
        if (activeTextUnitIndex < 0) {
            activeTextUnitIndex = pageSize - 1;
        } else if (activeTextUnitIndex >= pageSize) {
            activeTextUnitIndex = 0;
        }
        return activeTextUnitIndex;
    },

    /**
     * @param {TextUnit} textUnit Check if the textunit passed in is selected or not.
     * @returns {boolean} Returns true if textunit is selected, false otherwise.
     */
    isTextUnitSelected(textUnit) {
        let selectedTextUnitsMap = SearchResultsStore.getState().selectedTextUnitsMap;
        return typeof selectedTextUnitsMap[textUnit.getTextUnitKey()] !== "undefined";
    },

    /**
     * Shows the toolbar if search results exist.
     * @returns {boolean} true if results exist for the current search criteria. false otherwise.
     */
    mustToolbarBeShown() {
        let resultsStoreState = SearchResultsStore.getState();
        let paramsStoreState = SearchParamsStore.getState();
        return paramsStoreState.currentPageNumber > 1 || resultsStoreState.searchResults.length > 0;
    },

    /**
     * Retrieves the message key from Error.MESSAGEKEYS_MAP, populates it with values for any parameters it may
     * have and then returns the error message.
     * @returns {string} The error message with all the parameters populated.
     */
    getErrorMessage() {
        let errorObject = this.state.errorObject;
        let errorMessage = "";
        //TODO: fill in the error message with parameter values if any.
        if (errorObject !== null) {
            errorMessage = this.props.intl.formatMessage({
                id: Error.MESSAGEKEYS_MAP[errorObject.getErrorId()],
            });
        }
        return errorMessage;
    },

    /**
     * @returns {array} Array of TextUnit components for the current page of the search results
     */
    createTextUnitComponents() {
        return (
            <div className="textunits-container">
                {this.state.searchResults.map(this.createTextUnitComponent)}
            </div>
        );
    },

    /**
     *
     * @param {TextUnit} textUnit The sdk textunit object for which JSX must be returned.
     * @param {number} arrayIndex The index of the sdk textunit object in the search results array of the SearchResultsStore.
     * @returns {JSX} The JSX to paint the TextUnit component.
     */
    createTextUnitComponent(textUnit, arrayIndex) {
        return (
            <AltContainer key={this.getTextUnitComponentKey(textUnit)} stores={{ viewMode: ViewModeStore }}>
                <TextUnit
                    textUnit={textUnit}
                    textUnitIndex={arrayIndex}
                    translation={textUnit.getTarget()}
                    isActive={arrayIndex === this.state.activeTextUnitIndex}
                    isSelected={this.isTextUnitSelected(textUnit)}
                    onEditModeSetToTrue={this.onTextUnitEditModeSetToTrue}
                    onEditModeSetToFalse={this.onTextUnitEditModeSetToFalse}
                />
            </AltContainer>
        );
    },

    /**
     * @param {TextUnit} textUnit The textunit for which a unique key must be returned.
     * @returns {string} A string comprised of the textunit id, locale id and the variant id of the textunit.
     */
    getTextUnitComponentKey(textUnit) {
        // TODO: This function might not be required if the textunit component can be run purely through properties. Check if its possible to remove getInitialState() from TextUnit component
        /*
         * getTextUnitComponentKey() uses the getTmTextUnitVariantId as part of the key
         * because the getTmTextUnitVariantId changes after updating a textunit's translation.
         * This forces the TextUnit component getInitialState to be called. This is desired behavior
         * because getInitialState resets the TextUnit component state.
         */
        return (
            textUnit.getTmTextUnitId() +
            "_" +
            textUnit.getLocaleId() +
            textUnit.getTmTextUnitVariantId()
        );
    },

    getTextUnitCount(filteredItemCount, isCountRequestPending) {
        if (!filteredItemCount) {
            return null
        }

        if (isCountRequestPending) {
            return <div>Total text units: <DelayedSpinner className='mlxs' /></div>;
        }

        return <div>Total text units: { filteredItemCount }</div>;
    },

    /**
     * @returns {JSX} Depending on the state of the component, this function returns the JSX for the
     * workbench toolbar.
     */
    getTextUnitToolbarUI() {
        //TODO: create a WorkbenchToolbar component. The SearchResults component should delegate toolbar rendering and actions to the WorkbenchToolbar
        let ui = null;
        const isSearching = this.state.isSearching;
        const noMoreResults = this.state.noMoreResults;
        const filteredItemCount = this.state.filteredItemCount;
        const isCountRequestPending = this.state.isCountRequestPending;
        const isFirstPage = this.state.currentPageNumber <= 1;
        const selectedTextUnits = this.getSelectedTextUnits();
        const numberOfSelectedTextUnits = selectedTextUnits.length;
        const isAtLeastOneTextUnitSelected = numberOfSelectedTextUnits >= 1;

        const selectorCheckBoxDisabled = !AuthorityService.canEditTranslations();
        const canEditTranslations = AuthorityService.canEditTranslations();
        const actionButtonsDisabled = isSearching || !isAtLeastOneTextUnitSelected || !canEditTranslations;
        const nextPageButtonDisabled = isSearching || noMoreResults;
        const previousPageButtonDisabled = isSearching || isFirstPage;

        let pageSizes = [];
        for (let i of [10, 25, 50, 100]) {
            pageSizes.push(
                <MenuItem
                    key={i}
                    eventKey={i}
                    active={i == this.state.pageSize}
                    onSelect={(s, _) => WorkbenchActions.searchParamsChanged({changedParam: SearchConstants.PAGE_SIZE_CHANGED, pageSize: s})}
                >
                    {i}
                </MenuItem>
            );
        }

        const title = <FormattedMessage values={{"pageSize": this.state.pageSize}} id="search.unitsPerPage" />;

        if (this.state.mustShowToolbar) {
            ui = (
                <div>
                    <div>
                        <div className="pull-left">
                            <ButtonToolbar>
                                {canEditTranslations && (
                                    <Fragment>
                                        <Button
                                            bsSize="small"
                                            disabled={actionButtonsDisabled}
                                            onClick={
                                                this.onDeleteTextUnitsClicked
                                            }
                                        >
                                            <FormattedMessage id="label.delete" />
                                        </Button>
                                        <Button
                                            bsSize="small"
                                            bsStyle="primary"
                                            disabled={actionButtonsDisabled}
                                            onClick={
                                                this.onStatusTextUnitsClicked
                                            }
                                        >
                                            <FormattedMessage id="workbench.toolbar.status" />
                                        </Button>

                                        <DropdownButton
                                            bsSize="small"
                                            disabled={actionButtonsDisabled}
                                            noCaret
                                            id="dropdown-more-options"
                                            title={
                                                <span className="glyphicon glyphicon-option-horizontal"></span>
                                            }
                                        >
                                            <MenuItem header>
                                                <FormattedMessage id="workbench.toolbar.textUnitsAttribute" />
                                            </MenuItem>
                                            <MenuItem
                                                eventKey="1"
                                                onClick={
                                                    this.onDoNotTranslateClick
                                                }
                                            >
                                                <FormattedMessage id="workbench.toolbar.setTranslate" />
                                            </MenuItem>
                                        </DropdownButton>
                                    </Fragment>
                                )}
                            </ButtonToolbar>
                        </div>
                        <div
                            className="pull-right"
                            style={{
                                display: "flex",
                                alignItems: "center",
                                gap: "15px",
                            }}
                        >
                            {this.getTextUnitCount(filteredItemCount, isCountRequestPending)}
                            <AltContainer store={ViewModeStore}>
                                <ViewModeDropdown
                                    onModeSelected={(mode) =>
                                        ViewModeActions.changeViewMode(mode)
                                    }
                                />
                            </AltContainer>

                            {!selectorCheckBoxDisabled && (
                                <TextUnitSelectorCheckBox
                                    numberOfSelectedTextUnits={
                                        numberOfSelectedTextUnits
                                    }
                                    disabled={selectorCheckBoxDisabled}
                                />
                            )}

                            <DropdownButton
                                id="text-units-per-page"
                                title={title}
                            >
                                {pageSizes}
                            </DropdownButton>
                            <Button
                                bsSize="small"
                                disabled={previousPageButtonDisabled}
                                onClick={this.onFetchPreviousPageClicked}
                            >
                                <span className="glyphicon glyphicon-chevron-left"></span>
                            </Button>
                            <label className="default-label current-pageNumber">
                                {this.displayCurrentPageNumber()}
                            </label>
                            <Button
                                bsSize="small"
                                disabled={nextPageButtonDisabled}
                                onClick={this.onFetchNextPageClicked}
                            >
                                <span className="glyphicon glyphicon-chevron-right"></span>
                            </Button>
                        </div>
                    </div>

                    <div className="textunit-toolbar-clear" />
                </div>
            );
        }
        return ui;
    },

    /**
     * @returns {JSX} The JSX for the TextUnitsReviewModal if mustShowReviewModal is true, empty string otherwise.
     */
    getTextUnitsReviewModal() {
        let ui = "";
        if (this.state.mustShowReviewModal) {
            ui = (
                <TextUnitsReviewModal
                    isShowModal={this.state.mustShowReviewModal}
                    onReviewModalSaveClicked={this.onReviewModalSaveClicked}
                    textUnitsArray={this.getSelectedTextUnits()}
                    onCloseModal={this.hideReviewModal}
                />
            );
        }
        return ui;
    },

    /**
     * @returns {JSX} The JSX for the TranslateModal if showTranslateModal is true, empty string otherwise.
     */
    getTextUnitsTranslateModal() {
        let ui = "";
        if (this.state.showTranslateModal) {
            ui = (
                <TranslateModal
                    showModal={this.state.showTranslateModal}
                    selectedTextArray={this.getSelectedTextUnits()}
                    onSave={this.onTranslateModalSave}
                    onCancel={this.onTranslateModalCancel}
                />
            );
        }
        return ui;
    },

    /**
     * @returns {JSX} Return the currentPageNumber.
     */
    displayCurrentPageNumber() {
        return this.state.currentPageNumber;
    },

    /**
     * @returns {JSX}
     */
    getEmptyStateContainer() {
        let result = "";

        if (!this.state.isSearching) {
            if (!this.state.isReadyForSearching) {
                result = this.getEmptyStateContainerContent("search.result.selectRepoAndLocale");
            }
            else if (this.state.searchHadNoResults) {
                result = this.getEmptyStateContainerContent("search.result.empty");
            }
        }

        return result;
    },

    /**
     * @param {string} messageId message id of the message to be displayed
     * @returns {JSX}
     */
    getEmptyStateContainerContent(messageId) {
        return (
            <div className="empty-search-container text-center center-block">
                <div>
                    <FormattedMessage id={messageId} />
                </div>
                <img
                    className="empty-search-container-img"
                    src={magnifyingGlassSvg}
                />
            </div>
        );
    },

    render() {
        if (this.state.isSearching) {
            return (
                <div className="branch-spinner mtl mbl">
                    <DelayedSpinner />
                </div>
            );
        }

        return (
            <div onKeyUp={this.onKeyUpSearchResults} onClick={this.onChangeSearchResults}>
                {this.getTextUnitToolbarUI()}
                {this.createTextUnitComponents()}
                <DeleteConfirmationModal
                    showModal={this.state.showDeleteModal}
                    modalBodyMessage="textUnits.bulk.deleteMessage"
                    onDeleteCancelledCallback={this.onDeleteTextUnitsCancelled}
                    onDeleteClickedCallback={this.onDeleteTextUnitsConfirmed}
                />
                <ErrorModal
                    showModal={this.state.isErrorOccurred}
                    errorMessage={this.getErrorMessage()}
                    onErrorModalClosed={this.onErrorModalClosed}
                />
                {this.getTextUnitsTranslateModal()}
                {this.getEmptyStateContainer()}
                {this.getTextUnitsReviewModal()}
            </div>
        );
    },
});

export default injectIntl(SearchResults);
