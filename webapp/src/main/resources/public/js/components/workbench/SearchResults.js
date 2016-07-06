import _ from "lodash";
import FluxyMixin from "alt/mixins/FluxyMixin";
import keycode from "keycode";
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {ButtonGroup, ButtonToolbar, Button} from "react-bootstrap";
import Error from "../../utils/Error";
import DeleteConfirmationModal from "../widgets/DeleteConfirmationModal";
import ErrorModal from "../widgets/ErrorModal";
import SearchConstants from "../../utils/SearchConstants";
import SearchParamsStore from "../../stores/workbench/SearchParamsStore";
import SearchResultsStore from "../../stores/workbench/SearchResultsStore";
import TextUnit from "./TextUnit";
import TextUnitsReviewModal from "./TextUnitsReviewModal";
import TextUnitSelectorCheckBox from "./TextUnitSelectorCheckBox";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import ReviewTextUnitsDTO from "../../stores/workbench/ReviewTextUnitsDTO";

let SearchResults = React.createClass({

    mixins: [FluxyMixin],

    statics: {
        storeListeners: {
            "onSearchResultsStoreUpdated": SearchResultsStore
        }
    },

    /**
     * @returns {{
     *  searchResults: [],  Array of textunits in the current page of the search results.
     *  isFetchingPage: boolean,  Indicates status of fetching a page of search results from the server. Helps maintain pending state of the component.
     *  noMoreResults: boolean,  Indicates that no more results exist for the search criteria. Helps maintain enabled status of toolbar buttons.
     *  mustShowToolbar: boolean,  If no results exist for the provided search criteria, this boolean helps to hide the workbench toolbar.
     *  currentPageNumber: number,  Indicates the current page number of the search results.
     *  activeTextUnitIndex: number,  The index of the currently active textunit.
     *  showDeleteModal: boolean  Displays the DeleteConfirmationModal when the delete button is clicked.
     *  isErrorOccurred: boolean Helps show the ErrorModal if set to true.
     *  errorObject: object The Error object created by the store.
     *  errorResponse: object The error object returned by the promise. This can be used to pass parameters to translate the error. See getErrorMessage() in this file for details.
     *  }}
     */
    getInitialState: function () {

        let resultsStoreState = SearchResultsStore.getState();
        let searchParamsStoreState = SearchParamsStore.getState();
        return {
            "searchResults": resultsStoreState.searchResults,
            "isFetchingPage": false,
            "noMoreResults": false,
            "mustShowToolbar": false,
            "currentPageNumber": searchParamsStoreState.currentPageNumber,
            "activeTextUnitIndex": 0,
            "showDeleteModal": false,
            "mustShowReviewModal": false,
            "isErrorOccurred": false,
            "errorObject": null,
            "errorResponse": null
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
            case "r":
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
                "changedParam": SearchConstants.NEXT_PAGE_REQUESTED
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
            "mustShowReviewModal": true
        });
    },

    /**
     * Sets the state of this component to hide the TextUnitsReviewModal
     */
    hideReviewModal() {
        this.setState({
            "mustShowReviewModal": false
        });
    },

    /**
     * Set the state of this component to show the delete textunits modal
     */
    showDeleteModal() {
        this.setState({
            "showDeleteModal": true
        });
    },

    /**
     * Set the state of this component to hide the delete textunits modal
     */
    hideDeleteModal() {
        this.setState({
            "showDeleteModal": false
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
            "isErrorOccurred": false
        });
        WorkbenchActions.resetErrorState();
    },

    /**
     * Callback called when the SearchResultsStore state is updated.
     * Helps maintain the state of this component depending on the new state.
     * See comments on getInitialState() for how each attribute affects the rendering of this component.
     */
    onSearchResultsStoreUpdated: function () {

        let resultsStoreState = SearchResultsStore.getState();
        let paramsStoreState = SearchParamsStore.getState();
        let resultsInComponent = this.state.searchResults;
        let pageFetched = resultsStoreState.pageFetched;
        let mustShowToolbar = this.mustToolbarBeShown();

        this.setState({
            "searchResults": resultsStoreState.searchResults,
            "isFetchingPage": !pageFetched,
            "noMoreResults": resultsStoreState.noMoreResults,
            "currentPageNumber": paramsStoreState.currentPageNumber,
            "mustShowToolbar": mustShowToolbar,
            "activeTextUnitIndex": this.getActiveTextUnitIndex(),
            "isErrorOccurred": resultsStoreState.isErrorOccurred,
            "errorObject": resultsStoreState.errorObject
        });
    },

    /**
     * Called when the next page button is clicked on the workbench toolbar.
     * Sets the isFetchingPage attribute to true and fires the request to get the next page
     *  of search results.
     */
    onFetchNextPageClicked: function () {

        this.setPageLoadingStatus(true);
        if (!this.state.noMoreResults) {
            WorkbenchActions.searchParamsChanged({
                "changedParam": SearchConstants.NEXT_PAGE_REQUESTED
            });
        }
    },

    /**
     * Called when the previous page button is clicked on the workbench toolbar.
     * Sets the isFetchingPage attribute to true and fires the request to get the previous
     * page of search results if the currentPage is greater than one.
     */
    onFetchPreviousPageClicked() {

        if (this.state.currentPageNumber > 1) {
            this.setPageLoadingStatus(true);
            let paramData = {
                "changedParam": SearchConstants.PREVIOUS_PAGE_REQUESTED
            };
            WorkbenchActions.searchParamsChanged(paramData);
        }
    },

    /**
     * @param {number} activeTextUnitIndex The index to be set on the state of this component
     */
    setActiveTextUnitIndexState(activeTextUnitIndex) {
        this.setState({
            "activeTextUnitIndex": activeTextUnitIndex
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
            errorMessage = this.props.intl.formatMessage({ id: Error.MESSAGEKEYS_MAP[errorObject.getErrorId()] });
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
            <TextUnit key={this.getTextUnitComponentKey(textUnit)} wait={this.props.mustWait}
                      textUnit={textUnit} textUnitIndex={arrayIndex}
                      isActive={arrayIndex === this.state.activeTextUnitIndex}
                      isSelected={this.isTextUnitSelected(textUnit)}/>
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
        return textUnit.getTmTextUnitId() + '_' + textUnit.getLocaleId() + textUnit.getTmTextUnitVariantId();
    },

    /**
     * @param {boolean} boolValue If true is passed, then the component will display a pending status while waiting for
     * server to respond.
     */
    setPageLoadingStatus(boolValue) {
        this.setState({
            "isFetchingPage": boolValue
        });
    },

    /**
     * @returns {JSX} The JSX for the progress spinner to be displayed when server action is pending.
     */
    getLoadingSpinner: function () {
        let altMessage = this.props.intl.formatMessage({ id: "search.pagination.isLoading" });
        return (
            <img src="/img/ajax-loader.gif" alt={altMessage}/>
        );
    },

    /**
     * @returns {JSX} Depending on the state of the component, this function returns the JSX for the
     * workbench toolbar.
     */
    getTextUnitToolbarUI() {
        //TODO: create a WorkbenchToolbar component. The SearchResults component should delegate toolbar rendering and actions to the WorkbenchToolbar
        let ui = "";
        let isFetchingPage = this.state.isFetchingPage;
        let noMoreResults = this.state.noMoreResults;
        let isFirstPage = this.state.currentPageNumber <= 1;
        let selectedTextUnits = this.getSelectedTextUnits();
        let numberOfSelectedTextUnits = selectedTextUnits.length;
        let isAtLeastOneTextUnitSelected = numberOfSelectedTextUnits >= 1;

        let actionButtonsDisabled = isFetchingPage || !isAtLeastOneTextUnitSelected;
        let nextPageButtonDisabled = isFetchingPage || noMoreResults;
        let previousPageButtonDisabled = isFetchingPage || isFirstPage;

        if (this.state.mustShowToolbar) {
            ui = (
                <div>
                    <div>
                        <div className="pull-left">
                            <ButtonToolbar>
                                <Button bsSize="small" disabled={actionButtonsDisabled}
                                        onClick={this.onDeleteTextUnitsClicked}>
                                    <FormattedMessage id="label.delete" />
                                </Button>
                                <Button bsSize="small" bsStyle="primary" disabled={actionButtonsDisabled}
                                        onClick={this.onStatusTextUnitsClicked}>
                                    <FormattedMessage id="workbench.toolbar.status" />
                                </Button>
                            </ButtonToolbar>
                        </div>
                        <div className="pull-right">
                            <TextUnitSelectorCheckBox numberOfSelectedTextUnits={numberOfSelectedTextUnits}/>
                            <Button bsSize="small" disabled={previousPageButtonDisabled}
                                    onClick={this.onFetchPreviousPageClicked}><span
                                className="glyphicon glyphicon-chevron-left"></span></Button>
                            <label className="mls mrs default-label current-pageNumber">
                                {this.displayCurrentPageNumber()}
                            </label>
                            <Button bsSize="small" disabled={nextPageButtonDisabled}
                                    onClick={this.onFetchNextPageClicked}><span
                                className="glyphicon glyphicon-chevron-right"></span></Button>
                        </div>
                    </div>

                    <div className="textunit-toolbar-clear"/>
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
                <TextUnitsReviewModal isShowModal={this.state.mustShowReviewModal}
                                      onReviewModalSaveClicked={this.onReviewModalSaveClicked}
                                      textUnitsArray={this.getSelectedTextUnits()}
                                      onCloseModal={this.hideReviewModal}/>
            );
        }
        return ui;
    },

    /**
     * @returns {JSX} The progress spinner if server action is pending. Otherwise, return the currentPageNumber.
     */
    displayCurrentPageNumber() {
        let ui = this.state.currentPageNumber;
        if (this.state.isFetchingPage) {
            //TODO this is not good keeps doing network call (image no cache in springboot???) + flicker
            ui = this.getLoadingSpinner();
        }
        return ui;
    },

    render: function () {
        return (
            <div onKeyUp={this.onKeyUpSearchResults} onClick={this.onChangeSearchResults}>
                {this.getTextUnitToolbarUI()}
                {this.createTextUnitComponents()}
                <DeleteConfirmationModal showModal={this.state.showDeleteModal}
                                         modalBodyMessage="textUnits.bulk.deleteMessage"
                                         onDeleteCancelledCallback={this.onDeleteTextUnitsCancelled}
                                         onDeleteClickedCallback={this.onDeleteTextUnitsConfirmed}/>
                <ErrorModal showModal={this.state.isErrorOccurred}
                            errorMessage={this.getErrorMessage()}
                            onErrorModalClosed={this.onErrorModalClosed}/>
                {this.getTextUnitsReviewModal()}
            </div>
        );
    }

});

export default injectIntl(SearchResults);
