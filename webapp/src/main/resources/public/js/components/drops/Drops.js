import React from "react";
import {Alert, Button, ButtonGroup, ButtonToolbar, OverlayTrigger, Table, Tooltip} from "react-bootstrap";
import {IntlMixin, FormattedDate} from 'react-intl';
import FluxyMixin from "alt/mixins/FluxyMixin";

import PageRequestParams from "../../sdk/PageRequestParams";
import CancelDropConfig from "../../sdk/drop/CancelDropConfig";
import Drop from "../../sdk/drop/Drop";
import DropActions from "../../actions/drop/dropActions";
import DropStore from "../../stores/drop/DropStore";
import ImportDropConfig from "../../sdk/drop/ImportDropConfig";
import NewDropModal from "./NewDropModal";
import RepositoryActions from "../../actions/RepositoryActions";
import RepositoryStore from "../../stores/RepositoryStore";

let Drops = React.createClass({
    mixins: [IntlMixin, FluxyMixin],

    statics: {
        storeListeners: {
            "onDropStoreUpdated": DropStore
        }
    },

    /** @type {Number} */
    delayedRequestTimeout: null,

    getInitialState() {
        return {
            /** @type {Drop[]} */
            "ongoingDrops": [],
            /** @type {Drop[]} */
            "importedDrops": [],
            /** @type {Boolean} */
            "isImportedShown": false,
            /** @type {Boolean} */
            "isAlertShown": false,
            /** @type {String} */
            "alertMessage": null,
            /** @type {Boolean} */
            "isNewDropModalShown": false,          
            /** pagination related attributes  */
            /** @type {Number} */
            "pageSize": 10,
            /** @type {Number} */
            "onGoingCurrentPageNumber": 0,
            /** @type {Boolean} */
            "hasMoreOnGoingRequestResults": false,
            /** @type {Number} */
            "importedCurrentPageNumber": 0,
            /** @type {Boolean} */
            "hasMoreImportedRequestResults": false
        };
    },
    
    getCurrentPageRequestParam(currentPageNumber) {
        return new PageRequestParams(currentPageNumber, this.state.pageSize);
    },
    
    getPreviousPageRequestParam(currentPageNumber) {
        return new PageRequestParams(currentPageNumber - 1, this.state.pageSize);
    },
    
    getNextPageRequestParam(currentPageNumber) {
        return new PageRequestParams(currentPageNumber + 1, this.state.pageSize);
    },
    
    componentDidMount() {
        RepositoryActions.getAllRepositories();
        DropActions.getAllInProcess(this.getCurrentPageRequestParam(this.state.onGoingCurrentPageNumber)); 
        DropActions.getAllImported(this.getCurrentPageRequestParam(this.state.importedCurrentPageNumber)); 
    },

    componentWillUnmount() {

    },

    /**
     * @param {DropStore} dropStore
     */
    onDropStoreUpdated(dropStore) {

        let newReqs = dropStore.onGoingDrops;

        for (let newReq of newReqs) {
            if (newReq.status == Drop.STATUS_TYPE.SENDING ||
                newReq.status == Drop.STATUS_TYPE.IMPORTING) {
                // Because it's sending, we'll probably get a change of status soon, so let's make a new request again.
                this.delayGetNewRequests();
                break;
            }
        }

        this.setState({
            "ongoingDrops": dropStore.onGoingDrops,
            "importedDrops": dropStore.importedDrops,       
            "onGoingCurrentPageNumber": dropStore.onGoingCurrentPageNumber,
            "hasMoreOnGoingRequestResults": dropStore.hasMoreOnGoingRequestResults,
            "importedCurrentPageNumber": dropStore.importedCurrentPageNumber,
            "hasMoreImportedRequestResults": dropStore.hasMoreImportedRequestResults
        });
    },

    openNewRequestModal() {
        this.setState({
            "isNewDropModalShown": true
        });
    },

    /**
     * Closes the new drop modal
     */
    closeNewRequestModal() {
        this.setState({
            "isNewDropModalShown": false
        });

        this.delayGetNewRequests(500);
    },

    /**
     * @param {Number} delay in ms
     * Get all drops after a set timeout
     */
    delayGetNewRequests(delay = 5000) {
        if (this.delayedRequestTimeout) {
            clearTimeout(this.delayedRequestTimeout);
        }

        this.delayedRequestTimeout = setTimeout(() => {
            DropActions.getAllInProcess(this.getCurrentPageRequestParam(this.state.onGoingCurrentPageNumber)); 
            DropActions.getAllImported(this.getCurrentPageRequestParam(this.state.importedCurrentPageNumber)); 
        }, delay);
    },

    /**
     * Handle new request onclick
     */
    onClickNewRequest() {
        this.openNewRequestModal();
    },

    /**
     * Hande import onclick event
     */
    onClickImport(dropId, repoId) {
        this.showAlert(this.getIntlMessage("drops.beingImported.alert"));
        let importDropConfig = new ImportDropConfig(repoId, dropId, null);
        DropActions.importRequest(importDropConfig);

        this.delayGetNewRequests(500);
    },

    /**
     * handle cancel Drop onclick event
     * @param {Number} dropId
     * @param {Number} repoId
     */
    onClickCancel(dropId, repoId) {
        this.showAlert(this.getIntlMessage("drops.beingCanceled.alert"));
        let cancelDropConfig = new CancelDropConfig(dropId, null);
        DropActions.cancelRequest(cancelDropConfig);

        this.delayGetNewRequests(500);
    },

    /**
     * Convert rowData to markup
     *
     * @param {Drop} drop
     * @return {XML}
     */
    getTableRow(drop) {

        let status = "";

        switch (drop.status) {
            case Drop.STATUS_TYPE.IMPORTED:
                status = this.getIntlMessage("drops.status.imported");
                break;
            case Drop.STATUS_TYPE.IMPORTING:
                status = this.getIntlMessage("drops.status.importing");
                break;
            case Drop.STATUS_TYPE.IN_TRANSLATION:
                status = this.getIntlMessage("drops.status.inTranslation");
                break;
            case Drop.STATUS_TYPE.IN_REVIEW:
                status = this.getIntlMessage("drops.status.inReview");
                break;
            case Drop.STATUS_TYPE.SENDING:
                status = this.getIntlMessage("drops.status.sending");
                break;
            case Drop.STATUS_TYPE.CANCELED:
                status = this.getIntlMessage("drops.status.canceled");
                break;
        }

        return (
            <tr className="">
                <td>{drop.name}{this.getButtonControlBar(drop)}</td>
                <td>{drop.createdByUser.getDisplayName()}</td>
                <td><FormattedDate value={drop.createdDate} day="numeric" month="long" year="numeric"/></td>
                <td>{drop.repository.name}</td>
                <td>{status}</td>
            </tr>
        );
    },

    /**
     * Get control button toolbar
     * @param {Drop} drop
     * @return {JSX}
     */
    getButtonControlBar(drop) {
        let dropId = drop.id;
        let repoId = drop.repository.id;

        let importTitle = <Tooltip>{this.getIntlMessage("drops.controlbar.button.tooltip.import")}</Tooltip>;
        let cancelTitle = <Tooltip>{this.getIntlMessage("drops.controlbar.button.tooltip.cancel")}</Tooltip>;

        let cancelOverlay = "";
        if (!drop.isBeingExported() && !drop.isBeingImported()) {
            cancelOverlay = (<OverlayTrigger placement="top" overlay={cancelTitle}>
                <Button bsStyle="default" onClick={this.onClickCancel.bind(this, dropId, repoId)}>
                    <span className="glyphicon glyphicon-remove" aria-label={cancelTitle}/>
                </Button>
            </OverlayTrigger>);
        }

        return (
            <ButtonToolbar>
                <ButtonGroup>
                    <OverlayTrigger placement="top" overlay={importTitle}>
                        <Button bsStyle="default" onClick={this.onClickImport.bind(this, dropId, repoId)}>
                            <span className="glyphicon glyphicon-import" aria-label={importTitle}/>
                        </Button>
                    </OverlayTrigger>
                    {cancelOverlay}
                </ButtonGroup>
            </ButtonToolbar>
        );
    },

    /**
     * Table Header
     * @return {JSX}
     */
    getTableHeader() {
        return (
            <thead>
            <tr>
                <th>{this.getIntlMessage("drops.tableHeader.name")}</th>
                <th className="col-md-2">{this.getIntlMessage("drops.tableHeader.createdBy")}</th>
                <th className="col-md-2">{this.getIntlMessage("drops.tableHeader.createdDate")}</th>
                <th className="col-md-2">{this.getIntlMessage("drops.tableHeader.repository")}</th>
                <th className="col-md-2">{this.getIntlMessage("drops.tableHeader.status")}</th>
            </tr>
            </thead>
        );
    },

    /**
     * Table for the imported drops
     * @return {JSX}
     */
    getImportedTable() {
        let result;

        if (this.state.isImportedShown) {
            let tableClass = "drop-table imported-drop-table";
            let rows = this.state.importedDrops
                .filter(drop => drop.status !== Drop.STATUS_TYPE.CANCELED)
                .map(this.getTableRow);

            result = (
                <div>
                    <div className="drop-title-bar">
                        <h4 className="mrl title">{this.getIntlMessage("drops.importedRequests.title")}</h4>
                        {this.showImportedPagination()}
                    </div>
                    <Table className={tableClass}>
                        {this.getTableHeader()}
                        <tbody>
                        {rows}
                        </tbody>
                    </Table>
                </div>
            );
        }

        return result;
    },

    /**
     * Table for the on going drops
     * @return {JSX}
     */
    getOnGoingTable() {
        let tableClass = "drop-table ongoing-drop-table";
        let rows = this.state.ongoingDrops
            .filter(drop => drop.status !== Drop.STATUS_TYPE.CANCELED)
            .map(this.getTableRow);

        return (
            <div>
                <div className="drop-title-bar">
                    <h4 className="mrl title">{this.getIntlMessage("drops.ongoingRequests.title")}</h4>
                    {this.showOnGoingPagination()}
                    {this.getAlert()}
                    {this.getNewRequestButton()}
                </div>
                <Table className={tableClass}>
                    {this.getTableHeader()}
                    <tbody>
                    {rows}
                    </tbody>
                </Table>
            </div>
        );
    },

    /**
     * @param {Boolean} isGoingToShow True to show Imported. False to hide
     */
    onClickShowHideImportedLink(isGoingToShow) {
        this.setState({
            isImportedShown: isGoingToShow
        });
    },

    /**
     * Link to show or hide imported drops
     * @return {XML}
     */
    getShowHideImportedLink() {
        let linkButton;

        if (this.state.isImportedShown) {
            linkButton = (<Button bsStyle="link" className="pull-right"
                                  onClick={this.onClickShowHideImportedLink.bind(this, false)}>
                {this.getIntlMessage("drops.hideImported.link")}
            </Button>);
        } else {
            linkButton = (<Button bsStyle="link" className="pull-right"
                                  onClick={this.onClickShowHideImportedLink.bind(this, true)}>
                {this.getIntlMessage("drops.showImported.link")}
            </Button>);
        }

        return (<div>{linkButton}</div>);
    },
    
    /**
     * @return {JSX}
     */
    getNewRequestButton() {
        return (
            <div>
                <Button bsStyle="primary" bsSize="small" className="new-request-button"
                        onClick={this.onClickNewRequest}>
                    {this.getIntlMessage("drops.newRequest.btn")}
                </Button>
            </div>
        );
    },

    /**
     * Handler for when new translation request is made
     */
    onTranslationRequest() {
        this.showAlert(this.getIntlMessage("drops.newTranslationRequest.alert"));
    },

    /**
     * Handler for when new review request is made
     */
    onReviewRequest() {
        this.showAlert(this.getIntlMessage("drops.newReviewRequest.alert"));
    },

    /**
     * @return {XML}
     */
    getNewRequestModal() {
        return (<NewDropModal
                show={this.state.isNewDropModalShown}
                onClose={this.closeNewRequestModal}
                onTranslationRequest={this.onTranslationRequest}
                onReviewRequest={this.onReviewRequest}/>
        );
    },

    /**
     * Handle alert being dismissed
     */
    alertOnDismiss() {
        this.setState({
            "isAlertShown": false
        });
    },

    /**
     * @param {String} message
     * @param message
     */
    showAlert(message) {
        this.setState({
            "isAlertShown": true,
            "alertMessage": message
        });
    },

    /**
     * @return {JSX}
     */
    getAlert() {
        let alert = null;

        if (this.state.isAlertShown && this.state.alertMessage) {
            alert = (
                <div className="drop-alert text-center">
                    <Alert bsStyle="warning" onDismiss={this.alertOnDismiss} dismissAfter={5000}>
                        <span>{this.state.alertMessage}</span>
                    </Alert>
                </div>
            );
        }

        return alert;
    },

    onFetchPreviousOnGoingPageClicked() {
        
        if (this.state.onGoingCurrentPageNumber > 0) {
            DropActions.getAllInProcess(this.getPreviousPageRequestParam(this.state.onGoingCurrentPageNumber));  
        }      
    },
    
    onFetchNextOnGoingPageClicked() {

        if (this.state.hasMoreOnGoingRequestResults) {
            DropActions.getAllInProcess(this.getNextPageRequestParam(this.state.onGoingCurrentPageNumber)); 
        }
    },    
    
    onFetchPreviousImportedPageClicked() {

        if (this.state.importedCurrentPageNumber > 0) {
            DropActions.getAllImported(this.getPreviousPageRequestParam(this.state.importedCurrentPageNumber)); 
        }
    },
    
    onFetchNextImportedPageClicked() {

        if (this.state.hasMoreImportedRequestResults) {
            DropActions.getAllImported(this.getNextPageRequestParam(this.state.importedCurrentPageNumber));
        }
    },
    
    showOnGoingPagination() {
        let previousPageButtonDisabled = this.state.onGoingCurrentPageNumber == 0;
        let nextPageButtonDisabled = !this.state.hasMoreOnGoingRequestResults;
        let currentPageNumber = typeof this.state.onGoingCurrentPageNumber === "undefined" ? 0 : this.state.onGoingCurrentPageNumber;
        return (
            <div className="drop-pagination">
                <Button bsSize="small" className="mts mbs" disabled={previousPageButtonDisabled} onClick={this.onFetchPreviousOnGoingPageClicked}>
                    <span className="glyphicon glyphicon-chevron-left"></span>
                </Button>
                <label className="mls mrs default-label current-pageNumber">
                    {currentPageNumber + 1}
                </label>
                <Button bsSize="small" className="mts mbs" disabled={nextPageButtonDisabled} onClick={this.onFetchNextOnGoingPageClicked}>
                    <span className="glyphicon glyphicon-chevron-right"></span>
                </Button>
            </div>
        );
    },
    
    showImportedPagination() {
        let previousPageButtonDisabled = this.state.importedCurrentPageNumber == 0;
        let nextPageButtonDisabled = !this.state.hasMoreImportedRequestResults;
        let currentPageNumber = typeof this.state.importedCurrentPageNumber === "undefined" ? 0 : this.state.importedCurrentPageNumber;
        return (
            <div className="drop-pagination">
                <Button bsSize="small" className="mts mbs" disabled={previousPageButtonDisabled} onClick={this.onFetchPreviousImportedPageClicked}>
                    <span className="glyphicon glyphicon-chevron-left"></span>
                </Button>
                <label className="mls mrs default-label current-pageNumber">
                    {currentPageNumber + 1}
                </label>
                <Button bsSize="small" className="mts mbs" disabled={nextPageButtonDisabled} onClick={this.onFetchNextImportedPageClicked}>
                    <span className="glyphicon glyphicon-chevron-right"></span>
                </Button>
            </div>
        );
    },
    
    render() {
        return (
            <div>
                {this.getOnGoingTable()}
                {this.getShowHideImportedLink()}
                {this.getImportedTable()}
                {this.getNewRequestModal()}
            </div>
        );
    }

});

export default Drops;
